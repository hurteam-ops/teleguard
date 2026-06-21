-- =============================================================================
-- TeleFlow Database Schema — Initial Migration
-- =============================================================================

-- Users table: linked to Telegram accounts
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    telegram_id     BIGINT UNIQUE NOT NULL,
    username        TEXT DEFAULT '',
    first_name      TEXT DEFAULT '',
    last_name       TEXT DEFAULT '',
    photo_url       TEXT DEFAULT '',
    is_premium      BOOLEAN DEFAULT FALSE,
    premium_expires_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Sessions table: JWT session tracking
CREATE TABLE IF NOT EXISTS sessions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_id        UUID NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    revoked         BOOLEAN DEFAULT FALSE
);

-- Proxy audit log (minimal — no traffic data, just connection events)
CREATE TABLE IF NOT EXISTS connection_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    proxy_ip        TEXT,
    country_code    TEXT,
    connected_at    TIMESTAMPTZ DEFAULT NOW(),
    disconnected_at TIMESTAMPTZ,
    bytes_up        BIGINT DEFAULT 0,
    bytes_down      BIGINT DEFAULT 0
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_telegram_id ON users(telegram_id);
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_connection_log_user_id ON connection_log(user_id);
CREATE INDEX IF NOT EXISTS idx_connection_log_connected_at ON connection_log(connected_at);
