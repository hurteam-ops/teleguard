package main

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
	"go.uber.org/zap"

	"github.com/teleguard/teleguard/backend/internal/config"
	"github.com/teleguard/teleguard/backend/internal/handlers"
	"github.com/teleguard/teleguard/backend/internal/middleware"
	"github.com/teleguard/teleguard/backend/internal/repository"
	"github.com/teleguard/teleguard/backend/internal/service"
	"github.com/teleguard/teleguard/backend/internal/tg"
)

func main() {
	_ = godotenv.Load()

	logger, _ := zap.NewProduction()
	defer logger.Sync()
	sugar := logger.Sugar()

	cfg := config.Load(sugar)

	repo, err := repository.NewPostgresRepo(cfg.DatabaseURL)
	if err != nil {
		sugar.Fatalf("Failed to connect to database: %v", err)
	}
	defer repo.Close()

	redisClient, err := repository.NewRedisClient(cfg.RedisURL)
	if err != nil {
		sugar.Warnf("Redis not available (using in-memory rate limiter): %v", err)
	}
	_ = redisClient // In-memory limiter used instead

	proxyManager := service.NewProxyManager(sugar)
	proxyListB64 := os.Getenv("PROXY_LIST_B64")
	if proxyListB64 != "" {
		if err := proxyManager.LoadFromBase64(proxyListB64); err != nil {
			sugar.Warnf("Failed to load proxy list: %v", err)
		}
	} else {
		sugar.Warn("PROXY_LIST_B64 not set — running without proxies")
	}

	botService := tg.NewBotService(cfg.TelegramBotToken, sugar)
	botService.StartPolling()

	authService := service.NewAuthService(cfg.JWTSecret, repo, botService, sugar)

	router := gin.New()
	router.Use(gin.Recovery())
	router.Use(middleware.LoggerMiddleware(sugar))
	router.Use(middleware.CORSMiddleware(cfg.CORSOrigins))
	router.Use(middleware.RateLimitMiddleware(cfg.RateLimitRPM))

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":  "ok",
			"version": "1.0.0",
			"time":    time.Now().Unix(),
		})
	})

	v1 := router.Group("/api/v1")
	{
		authHandler := handlers.NewAuthHandler(authService, sugar)
		v1.POST("/auth/tg", authHandler.Authenticate)
		v1.POST("/auth/check-premium", authHandler.CheckPremium)

		protected := v1.Group("")
		protected.Use(middleware.JWTAuthMiddleware(cfg.JWTSecret))
		{
			configHandler := handlers.NewConfigHandler(proxyManager, sugar)
			protected.GET("/config", configHandler.GetConfig)
			protected.GET("/proxies", configHandler.ListProxies)
		}

		// Telegram bot webhook — validated with secret token
		tgGroup := v1.Group("/webhook/tg")
		tgGroup.Use(middleware.TelegramWebhookSecret(cfg.TGWebhookSecret))
		tgGroup.POST("", botHandler(botService, sugar))
	}

	srv := &http.Server{
		Addr:              fmt.Sprintf("%s:%s", cfg.APIHost, cfg.APIPort),
		Handler:           router,
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      30 * time.Second,
		IdleTimeout:       120 * time.Second,
		MaxHeaderBytes:    1 << 20, // 1 MB
	}

	go func() {
		sugar.Infof("TeleFlow API starting on %s:%s", cfg.APIHost, cfg.APIPort)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			sugar.Fatalf("Server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	sugar.Info("Shutting down server…")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		sugar.Fatalf("Server forced to shutdown: %v", err)
	}

	sugar.Info("Server exited")
}

func botHandler(botService *tg.BotService, sugar *zap.SugaredLogger) gin.HandlerFunc {
	return func(c *gin.Context) {
		var update tg.Update
		if err := c.ShouldBindJSON(&update); err != nil {
			sugar.Errorf("Failed to parse Telegram update: %v", err)
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request"})
			return
		}
		botService.HandleUpdate(update)
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	}
}
