package config

import (
	"fmt"
	"os"
	"strconv"

	"go.uber.org/zap"
)

type Config struct {
	TelegramBotToken string
	JWTSecret        string
	DatabaseURL      string
	RedisURL         string
	APIHost          string
	APIPort          string
	RateLimitRPM     int
	CORSOrigins      string
	LogLevel         string
	MinClientVersion string
	TGWebhookSecret  string
}

func Load(logger *zap.SugaredLogger) *Config {
	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" || jwtSecret == "default-secret-change-in-production" {
		logger.Fatalf("JWT_SECRET must be set to a random string (min 32 chars). Generate with: openssl rand -hex 64")
	}
	if len(jwtSecret) < 32 {
		logger.Fatalf("JWT_SECRET must be at least 32 characters. Current length: %d", len(jwtSecret))
	}

	corsOrigins := getEnv("CORS_ORIGINS", "https://api.teleflow.dev")
	tgWebhookSecret := os.Getenv("TG_WEBHOOK_SECRET")

	cfg := &Config{
		TelegramBotToken: getEnv("TELEGRAM_BOT_TOKEN", ""),
		JWTSecret:        jwtSecret,
		DatabaseURL:      getEnv("DATABASE_URL", "postgres://teleflow:password@localhost:5432/teleflow?sslmode=disable"),
		RedisURL:         getEnv("REDIS_URL", "redis://localhost:6379/0"),
		APIHost:          getEnv("API_HOST", "0.0.0.0"),
		APIPort:          getEnv("API_PORT", "8080"),
		RateLimitRPM:     getEnvInt("RATE_LIMIT_RPM", 30),
		CORSOrigins:      corsOrigins,
		LogLevel:         getEnv("LOG_LEVEL", "info"),
		MinClientVersion: getEnv("MIN_CLIENT_VERSION", "1.0.0"),
		TGWebhookSecret:  tgWebhookSecret,
	}

	if cfg.TelegramBotToken == "" {
		logger.Warn("TELEGRAM_BOT_TOKEN is not set — bot features will be disabled")
	}

	if corsOrigins == "*" {
		logger.Warn("CORS_ORIGINS is set to wildcard '*' — restrict in production")
	}

	return cfg
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getEnvInt(key string, defaultValue int) int {
	if value := os.Getenv(key); value != "" {
		if intVal, err := strconv.Atoi(value); err == nil {
			return intVal
		}
	}
	return defaultValue
}
