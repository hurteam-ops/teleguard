package handlers

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/teleguard/teleguard/backend/internal/service"
	"go.uber.org/zap"
)

type ConfigHandler struct {
	proxyManager *service.ProxyManager
	logger       *zap.SugaredLogger
}

func NewConfigHandler(proxyManager *service.ProxyManager, logger *zap.SugaredLogger) *ConfigHandler {
	return &ConfigHandler{proxyManager: proxyManager, logger: logger}
}

func (h *ConfigHandler) GetConfig(c *gin.Context) {
	proxies := h.proxyManager.GetProxies()
	if len(proxies) == 0 {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "no proxy servers available"})
		return
	}

	defaultProxy := h.proxyManager.GetDefaultProxy()

	c.JSON(http.StatusOK, gin.H{
		"proxies":      proxies,
		"defaultProxy": defaultProxy,
		"expiresAt":    time.Now().Add(2 * time.Hour).Unix(),
	})
}

func (h *ConfigHandler) ListProxies(c *gin.Context) {
	proxies := h.proxyManager.GetProxies()
	if proxies == nil {
		proxies = []gin.H{}
	}

	c.JSON(http.StatusOK, proxies)
}
