package main

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"log"
	"math/big"
	"net/http"
	"os"
	"sort"
	"strings"
	"sync"
	"time"
)

// ─── Models ──────────────────────────────────────────────────────────────

type AuthRequest struct {
	ID        int64  `json:"id"`
	FirstName string `json:"firstName"`
	LastName  string `json:"lastName"`
	Username  string `json:"username"`
	PhotoURL  string `json:"photoUrl"`
	AuthDate  int64  `json:"authDate"`
	Hash      string `json:"hash"`
}

type AuthResponse struct {
	Token     string `json:"token"`
	ExpiresAt int64  `json:"expiresAt"`
	User      User   `json:"user"`
}

type User struct {
	ID                   int64  `json:"id"`
	Username             string `json:"username"`
	FirstName            string `json:"firstName"`
	LastName             string `json:"lastName"`
	PhotoURL             string `json:"photoUrl"`
	IsPremium            bool   `json:"isPremium"`
	SubscriptionExpiresAt int64  `json:"subscriptionExpiresAt"`
	AuthToken            string `json:"authToken"`
	TokenExpiresAt       int64  `json:"tokenExpiresAt"`
}

type PremiumStatus struct {
	IsPremium bool  `json:"isPremium"`
	ExpiresAt int64 `json:"expiresAt"`
}

type InitResponse struct {
	Code string `json:"code"`
}

type PendingResponse struct {
	Status string      `json:"status"`
	Token  string      `json:"token,omitempty"`
	User   *User       `json:"user,omitempty"`
	Error  string      `json:"error,omitempty"`
}

// ─── Telegram Bot Models ─────────────────────────────────────────────────

type TGUpdate struct {
	UpdateID int64      `json:"update_id"`
	Message  *TGMessage `json:"message,omitempty"`
}

type TGMessage struct {
	MessageID int64  `json:"message_id"`
	From      *TGUser `json:"from,omitempty"`
	Chat      *TGChat `json:"chat"`
	Text      string `json:"text,omitempty"`
	Date      int64  `json:"date"`
}

type TGUser struct {
	ID           int64  `json:"id"`
	IsBot        bool   `json:"is_bot"`
	FirstName    string `json:"first_name"`
	LastName     string `json:"last_name,omitempty"`
	Username     string `json:"username,omitempty"`
	LanguageCode string `json:"language_code,omitempty"`
	IsPremium    *bool  `json:"is_premium,omitempty"`
}

type TGChat struct {
	ID   int64  `json:"id"`
	Type string `json:"type"`
}

type TGGetUpdatesResp struct {
	OK     bool        `json:"ok"`
	Result []TGUpdate  `json:"result"`
}

type TGSendMessageReq struct {
	ChatID    int64  `json:"chat_id"`
	Text      string `json:"text"`
	ParseMode string `json:"parse_mode,omitempty"`
}

// ─── In-Memory Storage ───────────────────────────────────────────────────

type pendingAuth struct {
	Code       string
	UserID     int64
	Username   string
	FirstName  string
	LastName   string
	IsPremium  bool
	CreatedAt  time.Time
	ClaimedAt  time.Time
	Claimed    bool
}

type authServer struct {
	botToken    string
	jwtSecret   []byte
	mu          sync.RWMutex
	pending     map[string]*pendingAuth
	issued      map[string]string // token -> user info
}

func newServer(botToken, jwtSecret string) *authServer {
	return &authServer{
		botToken:  botToken,
		jwtSecret: []byte(jwtSecret),
		pending:   make(map[string]*pendingAuth),
		issued:    make(map[string]string),
	}
}

// ─── Helpers ─────────────────────────────────────────────────────────────

func cors(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}
		next(w, r)
	}
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, map[string]string{"error": msg})
}

func randCode(n int) string {
	const letters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
	b := make([]byte, n)
	for i := range b {
		idx, _ := rand.Int(rand.Reader, big.NewInt(int64(len(letters))))
		b[i] = letters[idx.Int64()]
	}
	return string(b)
}

func randTokenBytes(n int) string {
	b := make([]byte, n)
	rand.Read(b)
	return hex.EncodeToString(b)
}

// ─── JWT (simple HMAC-based, no external lib) ───────────────────────────

func (s *authServer) signJWT(userID int64, username string, isPremium bool) (string, int64) {
	expiresAt := time.Now().Add(24 * time.Hour).Unix()
	header := base64.RawURLEncoding.EncodeToString([]byte(`{"alg":"HS256","typ":"JWT"}`))
	payload := base64.RawURLEncoding.EncodeToString(
		[]byte(fmt.Sprintf(`{"user_id":%d,"username":"%s","is_premium":%v,"exp":%d,"iat":%d}`,
			userID, username, isPremium, expiresAt, time.Now().Unix())),
	)
	sigInput := header + "." + payload
	mac := hmac.New(sha256.New, s.jwtSecret)
	mac.Write([]byte(sigInput))
	sig := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	return sigInput + "." + sig, expiresAt
}

func (s *authServer) verifySimpleJWT(token string) (map[string]interface{}, bool) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return nil, false
	}
	hdr, _ := base64.RawURLEncoding.DecodeString(parts[0])
	pl, _ := base64.RawURLEncoding.DecodeString(parts[1])
	sigInput := parts[0] + "." + parts[1]
	mac := hmac.New(sha256.New, s.jwtSecret)
	mac.Write([]byte(sigInput))
	expected := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	if subtle.ConstantTimeCompare([]byte(parts[2]), []byte(expected)) != 1 {
		return nil, false
	}

	var claims map[string]interface{}
	if err := json.Unmarshal(pl, &claims); err != nil {
		return nil, false
	}
	_ = hdr
	return claims, true
}

// ─── Telegram Auth HMAC Verification ────────────────────────────────────

func (s *authServer) verifyTelegramHash(req AuthRequest) bool {
	if req.Hash == "" || len(s.jwtSecret) == 0 {
		return false
	}
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
	data := sb.String()

	mac := hmac.New(sha256.New, []byte(s.botToken))
	mac.Write([]byte(data))
	expected := hex.EncodeToString(mac.Sum(nil))
	return subtle.ConstantTimeCompare([]byte(req.Hash), []byte(expected)) == 1
}

// ─── Telegram Bot Polling ────────────────────────────────────────────────

func (s *authServer) startPolling() {
	go func() {
		offset := int64(0)
		for {
			updates, err := s.getUpdates(offset, 5)
			if err != nil {
				log.Printf("Polling error: %v", err)
				time.Sleep(2 * time.Second)
				continue
			}
			for _, u := range updates {
				s.handleMessage(u.Message)
				offset = u.UpdateID + 1
			}
			time.Sleep(1 * time.Second)
		}
	}()
	log.Println("Bot polling started")
}

func (s *authServer) getUpdates(offset int64, timeout int) ([]TGUpdate, error) {
	url := fmt.Sprintf("https://api.telegram.org/bot%s/getUpdates?offset=%d&timeout=%d&allowed_updates=%s",
		s.botToken, offset, timeout, `["message"]`)
	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	var result struct {
		OK     bool       `json:"ok"`
		Result []TGUpdate `json:"result"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}
	if !result.OK {
		return nil, fmt.Errorf("telegram API not ok")
	}
	return result.Result, nil
}

func (s *authServer) sendMessage(chatID int64, text string) {
	payload, _ := json.Marshal(TGSendMessageReq{
		ChatID:    chatID,
		Text:      text,
		ParseMode: "HTML",
	})
	url := fmt.Sprintf("https://api.telegram.org/bot%s/sendMessage", s.botToken)
	http.Post(url, "application/json", strings.NewReader(string(payload)))
}

func (s *authServer) handleMessage(msg *TGMessage) {
	if msg == nil || msg.Text == "" {
		return
	}
	text := strings.TrimSpace(msg.Text)

	if text == "/start" || strings.HasPrefix(text, "/start ") {
		code := ""
		if strings.HasPrefix(text, "/start ") {
			code = strings.TrimSpace(strings.TrimPrefix(text, "/start "))
		}

		userID := int64(0)
		username := ""
		firstName := ""
		lastName := ""
		isPremium := false
		if msg.From != nil {
			userID = msg.From.ID
			username = msg.From.Username
			firstName = msg.From.FirstName
			lastName = msg.From.LastName
			if msg.From.IsPremium != nil {
				isPremium = *msg.From.IsPremium
			}
		}

		if code != "" {
			s.mu.Lock()
			if pa, ok := s.pending[code]; ok && !pa.Claimed {
				pa.Claimed = true
				pa.ClaimedAt = time.Now()
				pa.UserID = userID
				pa.Username = username
				pa.FirstName = firstName
				pa.LastName = lastName
				pa.IsPremium = isPremium
			} else if !ok {
				// Code not known yet — app will generate it locally and poll.
				// Create the entry as claimed since the user already messaged the bot.
				s.pending[code] = &pendingAuth{
					Code:      code,
					UserID:    userID,
					Username:  username,
					FirstName: firstName,
					LastName:  lastName,
					IsPremium: isPremium,
					CreatedAt: time.Now(),
					ClaimedAt: time.Now(),
					Claimed:   true,
				}
			}
			s.mu.Unlock()

			if msg.Chat != nil {
				s.sendMessage(msg.Chat.ID,
					fmt.Sprintf("✅ <b>Authentication successful!</b>\n\n"+
						"Welcome, %s! You can now return to the TeleFlow app.\n\n"+
						"Premium status: %s",
						firstName,
						map[bool]string{true: "✅ Active", false: "❌ Not active"}[isPremium]))
			}
		} else {
			if msg.Chat != nil {
				s.sendMessage(msg.Chat.ID,
					"🚀 <b>TeleFlow VPN</b>\n\n"+
						"Open the TeleFlow app to sign in.\n"+
						"The app will give you a code — send it here to authenticate.\n\n"+
						"<i>TeleFlow Pro requires Telegram Premium.</i>")
			}
		}
	}
}

// ─── Virtual Server Locations ────────────────────────────────────────────

type serverInfo struct {
	Country     string `json:"country"`
	CountryCode string `json:"countryCode"`
	City        string `json:"city"`
}

var locations = []serverInfo{
	{"Netherlands", "NL", "Amsterdam"},
	{"Germany", "DE", "Frankfurt"},
	{"United States", "US", "New York"},
	{"United States", "US", "Los Angeles"},
	{"Japan", "JP", "Tokyo"},
	{"Singapore", "SG", "Singapore"},
	{"United Kingdom", "GB", "London"},
	{"France", "FR", "Paris"},
	{"Canada", "CA", "Toronto"},
	{"Switzerland", "CH", "Zurich"},
	{"Sweden", "SE", "Stockholm"},
	{"Australia", "AU", "Sydney"},
	{"Brazil", "BR", "São Paulo"},
	{"India", "IN", "Mumbai"},
	{"South Korea", "KR", "Seoul"},
	{"Italy", "IT", "Milan"},
	{"Spain", "ES", "Madrid"},
	{"Poland", "PL", "Warsaw"},
	{"Russia", "RU", "Moscow"},
	{"United Arab Emirates", "AE", "Dubai"},
}

// ─── API Handlers ────────────────────────────────────────────────────────

func (s *authServer) handleHealth(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"status":  "ok",
		"version": "1.0.0",
		"time":    time.Now().Unix(),
	})
}

func (s *authServer) handleAuthTG(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		writeError(w, http.StatusMethodNotAllowed, "POST required")
		return
	}
	var req AuthRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid body")
		return
	}
	if req.ID == 0 {
		writeError(w, http.StatusBadRequest, "telegram ID required")
		return
	}
	if !s.verifyTelegramHash(req) {
		writeError(w, http.StatusUnauthorized, "invalid hash")
		return
	}
	if time.Now().Unix()-req.AuthDate > 300 {
		writeError(w, http.StatusUnauthorized, "auth data expired")
		return
	}

	premium := false
	// Check premium via getChatMember
	premium, _ = s.checkPremium(req.ID)

	token, expiresAt := s.signJWT(req.ID, req.Username, premium)

	user := User{
		ID:        req.ID,
		Username:  req.Username,
		FirstName: req.FirstName,
		LastName:  req.LastName,
		PhotoURL:  req.PhotoURL,
		IsPremium: premium,
		SubscriptionExpiresAt: func() int64 {
			if premium { return time.Now().Add(30 * 24 * time.Hour).Unix() }
			return 0
		}(),
		AuthToken:      token,
		TokenExpiresAt: expiresAt,
	}

	writeJSON(w, http.StatusOK, AuthResponse{
		Token:     token,
		ExpiresAt: expiresAt,
		User:      user,
	})
}

func (s *authServer) checkPremium(userID int64) (bool, error) {
	url := fmt.Sprintf("https://api.telegram.org/bot%s/getChatMember", s.botToken)
	payload := map[string]interface{}{
		"chat_id": userID,
		"user_id": userID,
	}
	body, _ := json.Marshal(payload)
	resp, err := http.Post(url, "application/json", strings.NewReader(string(body)))
	if err != nil {
		return false, err
	}
	defer resp.Body.Close()

	var result struct {
		OK     bool `json:"ok"`
		Result *struct {
			User struct {
				IsPremium *bool `json:"is_premium"`
			} `json:"user"`
		} `json:"result,omitempty"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return false, err
	}
	if result.OK && result.Result != nil && result.Result.User.IsPremium != nil {
		return *result.Result.User.IsPremium, nil
	}
	return false, nil
}

func (s *authServer) handleCheckPremium(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		writeError(w, http.StatusMethodNotAllowed, "POST required")
		return
	}
	auth := r.Header.Get("Authorization")
	if auth == "" || !strings.HasPrefix(auth, "Bearer ") {
		writeError(w, http.StatusUnauthorized, "missing token")
		return
	}
	claims, ok := s.verifySimpleJWT(strings.TrimPrefix(auth, "Bearer "))
	if !ok {
		writeError(w, http.StatusUnauthorized, "invalid token")
		return
	}

	userID, _ := claims["user_id"].(float64)
	premium, err := s.checkPremium(int64(userID))
	if err != nil {
		writeError(w, http.StatusInternalServerError, "premium check failed")
		return
	}

	exp := int64(0)
	if premium { exp = time.Now().Add(30 * 24 * time.Hour).Unix() }
	writeJSON(w, http.StatusOK, PremiumStatus{IsPremium: premium, ExpiresAt: exp})
}

func (s *authServer) handleAuthInit(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		writeError(w, http.StatusMethodNotAllowed, "POST required")
		return
	}
	code := randCode(6)
	s.mu.Lock()
	s.pending[code] = &pendingAuth{
		Code:      code,
		CreatedAt: time.Now(),
	}
	s.mu.Unlock()

	// Cleanup old codes
	go time.AfterFunc(10*time.Minute, func() {
		s.mu.Lock()
		delete(s.pending, code)
		s.mu.Unlock()
	})

	writeJSON(w, http.StatusOK, InitResponse{Code: code})
}

func (s *authServer) handlePendingCheck(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		writeError(w, http.StatusMethodNotAllowed, "GET required")
		return
	}
	code := strings.TrimPrefix(r.URL.Path, "/api/v1/auth/pending/")
	code = strings.TrimSuffix(code, "/")
	if code == "" {
		writeError(w, http.StatusBadRequest, "code required")
		return
	}

	s.mu.RLock()
	pa, ok := s.pending[code]
	s.mu.RUnlock()

	if !ok {
		writeJSON(w, http.StatusOK, PendingResponse{Status: "pending"})
		return
	}
	if !pa.Claimed {
		writeJSON(w, http.StatusOK, PendingResponse{Status: "pending"})
		return
	}

	token, expiresAt := s.signJWT(pa.UserID, pa.Username, pa.IsPremium)
	user := User{
		ID:        pa.UserID,
		Username:  pa.Username,
		FirstName: pa.FirstName,
		LastName:  pa.LastName,
		IsPremium: pa.IsPremium,
		SubscriptionExpiresAt: func() int64 {
			if pa.IsPremium { return time.Now().Add(30 * 24 * time.Hour).Unix() }
			return 0
		}(),
		AuthToken:      token,
		TokenExpiresAt: expiresAt,
	}

	writeJSON(w, http.StatusOK, PendingResponse{
		Status: "claimed",
		Token:  token,
		User:   &user,
	})
}

func (s *authServer) handleGetConfig(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		writeError(w, http.StatusMethodNotAllowed, "GET required")
		return
	}
	auth := r.Header.Get("Authorization")
	if auth == "" || !strings.HasPrefix(auth, "Bearer ") {
		writeError(w, http.StatusUnauthorized, "missing token")
		return
	}
	if _, ok := s.verifySimpleJWT(strings.TrimPrefix(auth, "Bearer ")); !ok {
		writeError(w, http.StatusUnauthorized, "invalid token")
		return
	}

	proxyB64 := os.Getenv("PROXY_LIST_B64")
	var proxies []map[string]interface{}
	if proxyB64 != "" {
		data, err := base64.StdEncoding.DecodeString(proxyB64)
		if err == nil {
			lines := strings.Split(string(data), "\n")
			for i, line := range lines {
				line = strings.TrimSpace(line)
				if line == "" { continue }
				parts := strings.Split(line, ":")
				if len(parts) >= 2 {
					loc := locations[i%len(locations)]
					n, _ := rand.Int(rand.Reader, big.NewInt(200))
					l, _ := rand.Int(rand.Reader, big.NewInt(80))
					p := map[string]interface{}{
						"ip": parts[0],
						"port": func() int {
							var p int
							fmt.Sscanf(parts[1], "%d", &p)
							return p
						}(),
						"country":     loc.Country,
						"countryCode": loc.CountryCode,
						"city":        loc.City,
						"latency":     n.Int64(),
						"load":        l.Int64(),
						"protocol":    "socks5",
						"isOnline":    true,
					}
					proxies = append(proxies, p)
				}
			}
		}
	}
	if proxies == nil {
		proxies = []map[string]interface{}{}
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"proxies":      proxies,
		"defaultProxy": "auto",
		"token":        "",
		"expiresAt":    0,
	})
}

func (s *authServer) handleGetProxies(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		writeError(w, http.StatusMethodNotAllowed, "GET required")
		return
	}
	auth := r.Header.Get("Authorization")
	if auth == "" || !strings.HasPrefix(auth, "Bearer ") {
		writeError(w, http.StatusUnauthorized, "missing token")
		return
	}
	if _, ok := s.verifySimpleJWT(strings.TrimPrefix(auth, "Bearer ")); !ok {
		writeError(w, http.StatusUnauthorized, "invalid token")
		return
	}

	proxyB64 := os.Getenv("PROXY_LIST_B64")
	var result []map[string]interface{}
	if proxyB64 != "" {
		data, err := base64.StdEncoding.DecodeString(proxyB64)
		if err == nil {
			lines := strings.Split(string(data), "\n")
			for i, line := range lines {
				line = strings.TrimSpace(line)
				if line == "" { continue }
				parts := strings.Split(line, ":")
				if len(parts) >= 2 {
					loc := locations[i%len(locations)]
					n, _ := rand.Int(rand.Reader, big.NewInt(200))
					l, _ := rand.Int(rand.Reader, big.NewInt(80))
					p := map[string]interface{}{
						"ip": parts[0],
						"port": func() int {
							var port int
							fmt.Sscanf(parts[1], "%d", &port)
							return port
						}(),
						"country":     loc.Country,
						"countryCode": loc.CountryCode,
						"city":        loc.City,
						"latency":     n.Int64(),
						"load":        l.Int64(),
						"protocol":    "socks5",
						"isOnline":    true,
					}
					result = append(result, p)
				}
			}
		}
	}
	if result == nil {
		result = []map[string]interface{}{}
	}
	writeJSON(w, http.StatusOK, result)
}

// ─── Main ────────────────────────────────────────────────────────────────

func main() {
	botToken := os.Getenv("TELEGRAM_BOT_TOKEN")
	jwtSecret := os.Getenv("JWT_SECRET")
	if botToken == "" {
		log.Fatal("TELEGRAM_BOT_TOKEN required")
	}
	if jwtSecret == "" {
		log.Fatal("JWT_SECRET required")
	}

	s := newServer(botToken, jwtSecret)
	s.startPolling()

	mux := http.NewServeMux()
	mux.HandleFunc("/health", cors(s.handleHealth))
	mux.HandleFunc("/api/v1/health", cors(s.handleHealth))
	mux.HandleFunc("/api/v1/auth/tg", cors(s.handleAuthTG))
	mux.HandleFunc("/api/v1/auth/check-premium", cors(s.handleCheckPremium))
	mux.HandleFunc("/api/v1/auth/init", cors(s.handleAuthInit))
	mux.HandleFunc("/api/v1/auth/pending/", cors(s.handlePendingCheck))
	mux.HandleFunc("/api/v1/config", cors(s.handleGetConfig))
	mux.HandleFunc("/api/v1/proxies", cors(s.handleGetProxies))

	port := os.Getenv("PORT")
	if port == "" { port = "8080" }

	log.Printf("Auth server starting on :%s", port)
	if err := http.ListenAndServe(":"+port, mux); err != nil {
		log.Fatal(err)
	}
}
