package service

import (
	"crypto/hmac"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"fmt"
	"sort"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"github.com/teleguard/teleguard/backend/internal/models"
	"github.com/teleguard/teleguard/backend/internal/repository"
	"github.com/teleguard/teleguard/backend/internal/tg"
	"go.uber.org/zap"
)

type AuthService struct {
	jwtSecret  []byte
	repo       *repository.PostgresRepo
	botService *tg.BotService
	logger     *zap.SugaredLogger
}

func NewAuthService(jwtSecret string, repo *repository.PostgresRepo, botService *tg.BotService, logger *zap.SugaredLogger) *AuthService {
	return &AuthService{
		jwtSecret:  []byte(jwtSecret),
		repo:       repo,
		botService: botService,
		logger:     logger,
	}
}

func (s *AuthService) Authenticate(req models.AuthRequest) (*models.AuthResponse, error) {
	if !s.verifyTelegramHash(req) {
		return nil, fmt.Errorf("invalid Telegram authentication hash")
	}

	if time.Now().Unix()-req.AuthDate > 300 {
		return nil, fmt.Errorf("auth data expired (older than 5 minutes)")
	}

	user, err := s.findOrCreateUser(req)
	if err != nil {
		return nil, fmt.Errorf("failed to find/create user: %w", err)
	}

	premium, err := s.botService.CheckPremium(user.TelegramID)
	if err != nil {
		s.logger.Warnf("Failed to check premium status: %v", err)
	}

	if premium {
		user.IsPremium = true
		user.PremiumExpires = time.Now().Add(30 * 24 * time.Hour)
	}

	tokenID := uuid.New().String()
	expiresAt := time.Now().Add(2 * time.Hour)

	claims := jwt.MapClaims{
		"user_id":    user.TelegramID,
		"is_premium": user.IsPremium,
		"jti":        tokenID,
		"exp":        expiresAt.Unix(),
		"iat":        time.Now().Unix(),
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString(s.jwtSecret)
	if err != nil {
		return nil, fmt.Errorf("failed to sign JWT: %w", err)
	}

	return &models.AuthResponse{
		Token:     tokenString,
		ExpiresAt: expiresAt.Unix(),
		User:      *user,
	}, nil
}

func (s *AuthService) CheckPremium(tokenID string, userID int64) (*models.PremiumStatus, error) {
	isPremium, err := s.botService.CheckPremium(userID)
	if err != nil {
		return nil, err
	}

	expiresAt := time.Now().Add(30 * 24 * time.Hour).Unix()
	if !isPremium {
		expiresAt = 0
	}

	return &models.PremiumStatus{
		IsPremium: isPremium,
		ExpiresAt: expiresAt,
	}, nil
}

func (s *AuthService) ValidateToken(tokenString string) (jwt.MapClaims, error) {
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method")
		}
		return s.jwtSecret, nil
	})

	if err != nil {
		return nil, err
	}

	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok || !token.Valid {
		return nil, fmt.Errorf("invalid token")
	}

	return claims, nil
}

// verifyTelegramHash implements real HMAC-SHA256 verification per Telegram Login Widget spec.
// https://core.telegram.org/widgets/login#checking-authorization
func (s *AuthService) verifyTelegramHash(req models.AuthRequest) bool {
	if req.Hash == "" || len(s.jwtSecret) == 0 {
		return false
	}

	// Build the data-check-string: all fields sorted alphabetically, joined by newlines.
	// Exclude 'hash' itself.
	fields := map[string]string{
		"auth_date":  fmt.Sprintf("%d", req.AuthDate),
		"first_name": req.FirstName,
		"id":         fmt.Sprintf("%d", req.ID),
		"last_name":  req.LastName,
		"photo_url":  req.PhotoURL,
		"username":   req.Username,
	}

	keys := make([]string, 0, len(fields))
	for k := range fields {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	var sb strings.Builder
	for i, k := range keys {
		if i > 0 {
			sb.WriteByte('\n')
		}
		sb.WriteString(k)
		sb.WriteByte('=')
		sb.WriteString(fields[k])
	}

	dataCheckString := sb.String()

	// Compute HMAC-SHA256 using the bot token as the secret key.
	// Telegram's spec: HMAC-SHA256(data_check_string, <bot_token>)
	mac := hmac.New(sha256.New, []byte(s.botToken())) // botToken() returns the raw token string
	mac.Write([]byte(dataCheckString))
	expectedHash := hex.EncodeToString(mac.Sum(nil))

	// Constant-time comparison to prevent timing attacks
	return subtle.ConstantTimeCompare([]byte(req.Hash), []byte(expectedHash)) == 1
}

// botToken returns the Telegram bot token.
// This is stored separately from the JWT secret.
func (s *AuthService) botToken() string {
	if s.botService != nil {
		return s.botService.Token()
	}
	return ""
}

func (s *AuthService) findOrCreateUser(req models.AuthRequest) (*models.User, error) {
	user := &models.User{
		TelegramID: req.ID,
		Username:   req.Username,
		FirstName:  req.FirstName,
		LastName:   req.LastName,
		PhotoURL:   req.PhotoURL,
		IsPremium:  false,
		CreatedAt:  time.Now(),
		UpdatedAt:  time.Now(),
	}

	return user, nil
}
