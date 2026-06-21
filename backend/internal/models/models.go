package models

import "time"

type User struct {
	ID              int64     `json:"id" db:"id"`
	TelegramID      int64     `json:"telegram_id" db:"telegram_id"`
	Username        string    `json:"username,omitempty" db:"username"`
	FirstName       string    `json:"first_name" db:"first_name"`
	LastName        string    `json:"last_name,omitempty" db:"last_name"`
	PhotoURL        string    `json:"photo_url,omitempty" db:"photo_url"`
	IsPremium       bool      `json:"is_premium" db:"is_premium"`
	PremiumExpires  time.Time `json:"premium_expires_at" db:"premium_expires_at"`
	CreatedAt       time.Time `json:"created_at" db:"created_at"`
	UpdatedAt       time.Time `json:"updated_at" db:"updated_at"`
}

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

type PremiumStatus struct {
	IsPremium bool  `json:"is_premium"`
	ExpiresAt int64 `json:"expires_at"`
}

type ProxyServer struct {
	IP          string `json:"ip"`
	Port        int    `json:"port"`
	Username    string `json:"username,omitempty"`
	Password    string `json:"password,omitempty"`
	Label       string `json:"label"`
	Country     string `json:"country"`
	CountryCode string `json:"countryCode"`
	City        string `json:"city"`
	Latency     int    `json:"latency"`
	Load        int    `json:"load"`
	Protocol    string `json:"protocol"`
	IsOnline    bool   `json:"isOnline"`
}

type ServerConfig struct {
	Proxies      []ProxyServer `json:"proxies"`
	DefaultProxy string        `json:"defaultProxy"`
	Token        string        `json:"token"`
	ExpiresAt    int64         `json:"expiresAt"`
}
