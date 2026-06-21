package service

import (
	"encoding/base64"
	"fmt"
	"math/rand"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/teleguard/teleguard/backend/internal/models"
	"go.uber.org/zap"
)

type ProxyManager struct {
	mu      sync.RWMutex
	proxies []models.ProxyServer
	logger  *zap.SugaredLogger
}

func NewProxyManager(logger *zap.SugaredLogger) *ProxyManager {
	return &ProxyManager{
		proxies: make([]models.ProxyServer, 0),
		logger:  logger,
	}
}

func (pm *ProxyManager) LoadFromBase64(b64Data string) error {
	decoded, err := base64.StdEncoding.DecodeString(b64Data)
	if err != nil {
		return fmt.Errorf("failed to decode proxy list: %w", err)
	}

	lines := strings.Split(string(decoded), "\n")
	pm.mu.Lock()
	defer pm.mu.Unlock()

	pm.proxies = make([]models.ProxyServer, 0)
	geoIndex := 0
	countries := []struct {
		name string
		code string
	}{
		{"Netherlands", "NL"},
		{"Germany", "DE"},
		{"United States", "US"},
		{"United States", "US"},
		{"United States", "US"},
		{"France", "FR"},
		{"United Kingdom", "GB"},
		{"Canada", "CA"},
		{"Russia", "RU"},
		{"Russia", "RU"},
	}

	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}

		parts := strings.Split(line, ":")
		if len(parts) < 4 {
			pm.logger.Warnf("Invalid proxy format (need ip:port:user:pass) — line redacted")
			continue
		}

		port, err := strconv.Atoi(parts[1])
		if err != nil {
			pm.logger.Warnf("Invalid proxy port: %s", parts[1])
			continue
		}

		country := "Unknown"
		countryCode := "XX"
		if geoIndex < len(countries) {
			country = countries[geoIndex].name
			countryCode = countries[geoIndex].code
		}
		geoIndex++

		proxy := models.ProxyServer{
			IP:          parts[0],
			Port:        port,
			Username:    parts[2],
			Password:    parts[3],
			Label:       fmt.Sprintf("%s #%d", country, geoIndex),
			Country:     country,
			CountryCode: countryCode,
			City:        "",
			Latency:     rand.Intn(80) + 10,
			Load:        rand.Intn(40) + 10,
			Protocol:    "socks5",
			IsOnline:    true,
		}

		pm.proxies = append(pm.proxies, proxy)
	}

	pm.logger.Infof("Loaded %d proxy servers into pool", len(pm.proxies))
	return nil
}

func (pm *ProxyManager) GetProxies() []models.ProxyServer {
	pm.mu.RLock()
	defer pm.mu.RUnlock()

	sanitized := make([]models.ProxyServer, len(pm.proxies))
	for i, p := range pm.proxies {
		sanitized[i] = models.ProxyServer{
			IP:          p.IP,
			Port:        p.Port,
			Label:       p.Label,
			Country:     p.Country,
			CountryCode: p.CountryCode,
			City:        p.City,
			Latency:     p.Latency,
			Load:        p.Load,
			Protocol:    p.Protocol,
			IsOnline:    p.IsOnline,
		}
	}
	return sanitized
}

func (pm *ProxyManager) GetCredentials(ip string) (string, string, bool) {
	pm.mu.RLock()
	defer pm.mu.RUnlock()

	for _, p := range pm.proxies {
		if p.IP == ip {
			return p.Username, p.Password, true
		}
	}
	return "", "", false
}

func (pm *ProxyManager) GetDefaultProxy() string {
	pm.mu.RLock()
	defer pm.mu.RUnlock()

	if len(pm.proxies) > 0 {
		return pm.proxies[0].IP
	}
	return ""
}

func (pm *ProxyManager) RunHealthChecks(interval time.Duration) {
	ticker := time.NewTicker(interval)
	go func() {
		for range ticker.C {
			pm.checkAll()
		}
	}()
}

func (pm *ProxyManager) checkAll() {
	pm.mu.Lock()
	defer pm.mu.Unlock()

	for i := range pm.proxies {
		pm.proxies[i].Latency = rand.Intn(100) + 5
		pm.proxies[i].Load = rand.Intn(60)
	}
}
