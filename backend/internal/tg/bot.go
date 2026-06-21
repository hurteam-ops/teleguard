package tg

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"go.uber.org/zap"
)

type Update struct {
	UpdateID         int64            `json:"update_id"`
	Message          *Message         `json:"message,omitempty"`
	CallbackQuery    *CallbackQuery   `json:"callback_query,omitempty"`
	PreCheckoutQuery *PreCheckoutQuery `json:"pre_checkout_query,omitempty"`
}

type Message struct {
	MessageID int64  `json:"message_id"`
	From      *User  `json:"from,omitempty"`
	Chat      *Chat  `json:"chat"`
	Text      string `json:"text,omitempty"`
	Date      int64  `json:"date"`
}

type User struct {
	ID           int64  `json:"id"`
	IsBot        bool   `json:"is_bot"`
	FirstName    string `json:"first_name"`
	LastName     string `json:"last_name,omitempty"`
	Username     string `json:"username,omitempty"`
	LanguageCode string `json:"language_code,omitempty"`
	IsPremium    *bool  `json:"is_premium,omitempty"`
}

type Chat struct {
	ID   int64  `json:"id"`
	Type string `json:"type"`
}

type CallbackQuery struct {
	ID      string   `json:"id"`
	From    User     `json:"from"`
	Data    string   `json:"data,omitempty"`
	Message *Message `json:"message,omitempty"`
}

type PreCheckoutQuery struct {
	ID             string `json:"id"`
	From           User   `json:"from"`
	Currency       string `json:"currency"`
	TotalAmount    int    `json:"total_amount"`
	InvoicePayload string `json:"invoice_payload"`
}

type BotService struct {
	token  string
	client *http.Client
	logger *zap.SugaredLogger
}

func NewBotService(token string, logger *zap.SugaredLogger) *BotService {
	return &BotService{
		token: token,
		client: &http.Client{
			Timeout: 10 * time.Second,
		},
		logger: logger,
	}
}

func (b *BotService) Token() string {
	return b.token
}

func (b *BotService) StartPolling() {
	if b.token == "" {
		b.logger.Warn("Telegram bot token not set — skipping polling")
		return
	}

	go func() {
		offset := int64(0)
		for {
			updates, err := b.getUpdates(offset, 30)
			if err != nil {
				b.logger.Errorf("Failed to get updates: %v", err)
				time.Sleep(5 * time.Second)
				continue
			}

			for _, update := range updates {
				b.HandleUpdate(update)
				offset = update.UpdateID + 1
			}

			time.Sleep(1 * time.Second)
		}
	}()

	b.logger.Info("Telegram bot polling started")
}

func (b *BotService) HandleUpdate(update Update) {
	if update.Message != nil {
		b.handleMessage(update.Message)
	}
	if update.CallbackQuery != nil {
		b.handleCallbackQuery(update.CallbackQuery)
	}
}

func (b *BotService) CheckPremium(userID int64) (bool, error) {
	if b.token == "" {
		return false, fmt.Errorf("bot token not configured")
	}

	url := fmt.Sprintf("https://api.telegram.org/bot%s/getChatMember", b.token)
	payload := map[string]interface{}{
		"chat_id": userID,
		"user_id": userID,
	}

	body, _ := json.Marshal(payload)
	resp, err := b.client.Post(url, "application/json", bytes.NewReader(body))
	if err != nil {
		return false, err
	}
	defer resp.Body.Close()

	var result struct {
		OK     bool `json:"ok"`
		Result *struct {
			User User `json:"user"`
		} `json:"result,omitempty"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return false, err
	}

	if result.OK && result.Result != nil {
		if result.Result.User.IsPremium != nil {
			return *result.Result.User.IsPremium, nil
		}
	}

	return false, nil
}

func (b *BotService) SendMessage(chatID int64, text string) error {
	url := fmt.Sprintf("https://api.telegram.org/bot%s/sendMessage", b.token)
	payload := map[string]interface{}{
		"chat_id":    chatID,
		"text":       text,
		"parse_mode": "HTML",
	}

	body, _ := json.Marshal(payload)
	resp, err := b.client.Post(url, "application/json", bytes.NewReader(body))
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	return nil
}

func (b *BotService) handleMessage(msg *Message) {
	if msg.Text == "/start" {
		welcome := fmt.Sprintf(
			"\uD83D\uDE80 <b>Welcome to TeleFlow!</b>\n\n"+
				"To activate your VPN subscription:\n\n"+
				"1. Open TeleFlow app\n"+
				"2. Tap \"Sign in with Telegram\"\n"+
				"3. You're all set!\n\n"+
				"<i>TeleFlow Pro requires Telegram Premium subscription.</i>\n"+
				"Current status: <b>Checking…</b>",
		)
		if err := b.SendMessage(msg.Chat.ID, welcome); err != nil {
			b.logger.Errorf("Failed to send welcome: %v", err)
		}

		isPremium := false
		if msg.From != nil && msg.From.IsPremium != nil {
			isPremium = *msg.From.IsPremium
		}

		statusMsg := "❌ Telegram Premium not detected."
		if isPremium {
			statusMsg = "✅ <b>Telegram Premium verified!</b> You have full TeleFlow Pro access."
		}

		if err := b.SendMessage(msg.Chat.ID, statusMsg); err != nil {
			b.logger.Errorf("Failed to send premium status: %v", err)
		}
	}
}

func (b *BotService) handleCallbackQuery(cq *CallbackQuery) {
	switch cq.Data {
	case "check_premium":
		isPremium := false
		if cq.From.IsPremium != nil {
			isPremium = *cq.From.IsPremium
		}
		text := "❌ No Premium subscription detected."
		if isPremium {
			text = "✅ Premium subscription is active! Enjoy TeleFlow Pro."
		}
		b.SendMessage(cq.Message.Chat.ID, text)
	}
}

func (b *BotService) getUpdates(offset int64, timeout int) ([]Update, error) {
	url := fmt.Sprintf(
		"https://api.telegram.org/bot%s/getUpdates?offset=%d&timeout=%d",
		b.token, offset, timeout,
	)

	resp, err := b.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	var result struct {
		OK     bool     `json:"ok"`
		Result []Update `json:"result"`
	}

	if err := json.Unmarshal(body, &result); err != nil {
		return nil, err
	}

	if !result.OK {
		return nil, fmt.Errorf("telegram API returned not OK")
	}

	return result.Result, nil
}
