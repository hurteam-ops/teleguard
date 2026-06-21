# TeleFlow

<p align="center">
  <img src="docs/logo.png" alt="TeleFlow Logo" width="120" height="120"/>
</p>

<p align="center">
  <strong>Freedom. Security. Privacy. — Like Telegram, for VPN.</strong>
</p>

<p align="center">
  <a href="https://github.com/teleguard/teleguard"><img src="https://img.shields.io/badge/version-1.0.0-blue" alt="Version"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="License"/></a>
  <a href="https://github.com/teleguard/teleguard/actions"><img src="https://img.shields.io/github/actions/workflow/status/teleguard/teleguard/build.yml" alt="Build"/></a>
</p>

---

## 📡 What is TeleFlow?

TeleFlow is an open-source privacy-focused VPN application that uses the **same design philosophy as Telegram** — clean, fast, secure, and freedom-oriented. Subscription is managed through a **Telegram Bot**: if you have **Telegram Premium**, you get TeleFlow Pro. Simple.

| Feature | Free | Pro (TG Premium) |
|---|---|---|
| Core VPN (proxy-based) | ✅ | ✅ |
| Country selection | ❌ | ✅ |
| Best speed servers | ❌ | ✅ |
| No-logs policy | ✅ | ✅ |
| Kill switch | ✅ | ✅ |
| Split tunneling | ❌ | ✅ |
| Multi-platform | Android | Android + iOS/Desktop (coming) |

---

## 🔒 Security Model

Security is architected like **Mullvad** — minimal data, maximum privacy, full transparency.

### Core Principles
- **No logs** — Zero connection logs stored. No timestamps, no IP records, no traffic inspection.
- **Short-lived credentials** — Proxy auth tokens rotated every 2 hours.
- **Server-side secrets** — Proxy credentials never touch client source code or GitHub.
- **Kill switch** — All non-VPN traffic blocked if connection drops.
- **DNS leak protection** — All DNS queries forced through encrypted tunnel.

### Architecture
```
[Client] ←--- Auth via Telegram Bot ---→ [TeleFlow API Gateway]
    │                                          │
    │ ←--- Proxy config (JWT, 2h TTL) ---→    │
    │                                          │
    ▼                                          ▼
[Local Tunnel (TUN → SOCKS5)]        [Proxy Pool Manager]
    │                                          │
    │ ←--- Encrypted SOCKS5 ---→            [Geo-Tagged Proxies]
    │                                          │
    ▼                                          ▼
[Internet]                               [Health Checks + Load Balancer]
```

---

## 🛡️ Technology Stack

| Component | Technology |
|---|---|
| **Android Client** | Kotlin, Jetpack Compose |
| **VPN Engine** | tun2socks + upstream SOCKS5 |
| **Backend API** | Go (Gin), PostgreSQL, Redis |
| **Telegram Integration** | Telegram Bot API + Login Widget |
| **Proxy Protocol** | SOCKS5 (with optional TLS wrapping) |
| **CI/CD** | GitHub Actions |
| **Infrastructure** | Docker, fly.io / Hetzner |

---

## 🚀 Getting Started

### Prerequisites
- Android 8.0+
- Telegram account with Premium status (for Pro features)
- Telegram Bot Token (from [@BotFather](https://t.me/botfather))

### Quick Start

1. **Clone the repo**
   ```bash
   git clone https://github.com/teleguard/teleguard.git
   cd teleguard
   ```

2. **Configure secrets**  
   Copy `.env.example` to `.env` and fill in your values:
   ```bash
   cp .env.example .env
   ```
   Required variables:
   - `TELEGRAM_BOT_TOKEN` — from BotFather
   - `PROXY_LIST` — base64-encoded proxy credentials
   - `JWT_SECRET` — random 64-char hex string
   - `DATABASE_URL` — PostgreSQL connection string

3. **Run the backend**
   ```bash
   docker compose up -d
   ```

4. **Build the Android app**
   ```bash
   cd android
   ./gradlew assembleRelease
   ```

---

## 📱 Android App Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/teleflow/
│   │   │   ├── ui/           # Compose UI (Telegram-inspired theme)
│   │   │   │   ├── screens/  # Main, Settings, Connection, ProxyList
│   │   │   │   ├── components/ # Reusable UI components
│   │   │   │   └── theme/    # Colors, typography, Telegram palette
│   │   │   ├── vpn/          # VpnService + tun2socks integration
│   │   │   ├── data/         # Repository, API, local storage
│   │   │   ├── service/      # Background services
│   │   │   ├── util/         # Helpers, extensions
│   │   │   └── viewmodel/    # ViewModels
│   │   ├── res/              # Resources (drawables, strings)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build-logic/              # Convention plugins
└── gradle/
```

---

## 🌐 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/tg` | Authenticate via Telegram Login Widget |
| `POST` | `/api/v1/auth/check-premium` | Verify Telegram Premium status |
| `GET`  | `/api/v1/config` | Get proxy configuration (JWT-protected) |
| `GET`  | `/api/v1/proxies` | List available proxy locations |
| `GET`  | `/api/v1/health` | Service health check |

---

## 🔐 Security Disclosure

TeleFlow is open source by design. If you find a vulnerability, please open a draft security advisory on GitHub — do not file a public issue.

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

<p align="center">
  <i>Inspired by Telegram's mission of freedom and privacy. Built for the open web.</i>
</p>
