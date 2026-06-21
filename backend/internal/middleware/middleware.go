package middleware

import (
	"crypto/subtle"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"go.uber.org/zap"
)

func LoggerMiddleware(logger *zap.SugaredLogger) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		path := c.Request.URL.Path

		c.Next()

		latency := time.Since(start)
		status := c.Writer.Status()

		logger.Infow("HTTP request",
			"method", c.Request.Method,
			"path", path,
			"status", status,
			"latency", latency,
			"ip", c.ClientIP(),
		)
	}
}

func CORSMiddleware(origins string) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", origins)
		c.Header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Content-Type, Authorization")
		c.Header("Access-Control-Max-Age", "86400")
		c.Header("Access-Control-Allow-Credentials", "true")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}

		c.Next()
	}
}

func JWTAuthMiddleware(secret string) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "authorization header required"})
			return
		}

		parts := strings.SplitN(authHeader, " ", 2)
		if len(parts) != 2 || parts[0] != "Bearer" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "invalid authorization format"})
			return
		}

		tokenString := parts[1]

		token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, jwt.ErrSignatureInvalid
			}
			return []byte(secret), nil
		})

		if err != nil || !token.Valid {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "invalid or expired token"})
			return
		}

		claims, ok := token.Claims.(jwt.MapClaims)
		if !ok {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "invalid token claims"})
			return
		}

		if userID, ok := claims["user_id"].(float64); ok {
			c.Set("user_id", int64(userID))
		}
		if isPremium, ok := claims["is_premium"].(bool); ok {
			c.Set("is_premium", isPremium)
		}
		if tokenID, ok := claims["jti"].(string); ok {
			c.Set("token_id", tokenID)
		}

		c.Next()
	}
}

type rateLimiter struct {
	mu      sync.Mutex
	clients map[string]*clientBucket
	rpm     int
}

type clientBucket struct {
	count   int
	resetAt time.Time
}

var limiter = &rateLimiter{
	clients: make(map[string]*clientBucket),
}

func RateLimitMiddleware(rpm int) gin.HandlerFunc {
	if rpm <= 0 {
		rpm = 30
	}
	limiter.rpm = rpm

	go func() {
		for {
			time.Sleep(time.Minute)
			limiter.mu.Lock()
			now := time.Now()
			for k, v := range limiter.clients {
				if now.After(v.resetAt) {
					delete(limiter.clients, k)
				}
			}
			limiter.mu.Unlock()
		}
	}()

	return func(c *gin.Context) {
		ip := c.ClientIP()
		limiter.mu.Lock()

		bucket, exists := limiter.clients[ip]
		now := time.Now()

		if !exists || now.After(bucket.resetAt) {
			limiter.clients[ip] = &clientBucket{
				count:   1,
				resetAt: now.Add(time.Minute),
			}
			limiter.mu.Unlock()
			c.Next()
			return
		}

		bucket.count++
		if bucket.count > limiter.rpm {
			limiter.mu.Unlock()
			c.AbortWithStatusJSON(http.StatusTooManyRequests, gin.H{
				"error": "rate limit exceeded",
			})
			return
		}

		limiter.mu.Unlock()
		c.Next()
	}
}

func TelegramWebhookSecret(secret string) gin.HandlerFunc {
	if secret == "" {
		return func(c *gin.Context) { c.Next() }
	}

	return func(c *gin.Context) {
		header := c.GetHeader("X-Telegram-Bot-Api-Secret-Token")
		if header == "" {
			c.AbortWithStatusJSON(http.StatusForbidden, gin.H{"error": "missing secret token"})
			return
		}

		if subtle.ConstantTimeCompare([]byte(header), []byte(secret)) != 1 {
			c.AbortWithStatusJSON(http.StatusForbidden, gin.H{"error": "invalid secret token"})
			return
		}

		c.Next()
	}
}
