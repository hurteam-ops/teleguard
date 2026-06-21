package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/teleguard/teleguard/backend/internal/models"
	"github.com/teleguard/teleguard/backend/internal/service"
	"go.uber.org/zap"
)

type AuthHandler struct {
	authService *service.AuthService
	logger      *zap.SugaredLogger
}

func NewAuthHandler(authService *service.AuthService, logger *zap.SugaredLogger) *AuthHandler {
	return &AuthHandler{authService: authService, logger: logger}
}

func (h *AuthHandler) Authenticate(c *gin.Context) {
	var req models.AuthRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body"})
		return
	}

	if req.ID == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "telegram ID is required"})
		return
	}

	response, err := h.authService.Authenticate(req)
	if err != nil {
		h.logger.Errorf("Authentication failed: %v", err)
		c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, response)
}

func (h *AuthHandler) CheckPremium(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not identified"})
		return
	}

	tokenID, _ := c.Get("token_id")
	tokenIDStr, _ := tokenID.(string)

	status, err := h.authService.CheckPremium(tokenIDStr, userID.(int64))
	if err != nil {
		h.logger.Errorf("Premium check failed: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "premium check failed"})
		return
	}

	c.JSON(http.StatusOK, status)
}
