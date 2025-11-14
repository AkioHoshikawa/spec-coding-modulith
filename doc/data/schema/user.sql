-- ============================================================================
-- ユーザー・認証ドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- users: ユーザーアカウント
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(255) NOT NULL, -- Argon2/PBKDF2でハッシュ化
    phone_number VARCHAR(20),
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    gender VARCHAR(20), -- MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    user_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, DELETED
    user_role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER', -- CUSTOMER, ADMIN, SUPPORT
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(255), -- TOTP secret (暗号化保存)
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip VARCHAR(45),
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 1 -- 楽観ロック用
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(user_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at ON users(created_at);

COMMENT ON TABLE users IS 'ユーザーアカウントマスタ。個人情報を含むため高セキュリティで管理';
COMMENT ON COLUMN users.password_hash IS 'Argon2またはPBKDF2でハッシュ化されたパスワード';
COMMENT ON COLUMN users.mfa_secret IS 'TOTP用のシークレット（暗号化して保存）';
COMMENT ON COLUMN users.version IS '楽観ロック用バージョン番号';

-- ----------------------------------------------------------------------------
-- user_addresses: 配送先住所
-- ----------------------------------------------------------------------------
CREATE TABLE user_addresses (
    address_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    address_type VARCHAR(20) NOT NULL DEFAULT 'SHIPPING', -- SHIPPING, BILLING
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    recipient_name VARCHAR(200) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    prefecture VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    phone_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_addresses_is_default ON user_addresses(user_id, is_default) WHERE deleted_at IS NULL;

COMMENT ON TABLE user_addresses IS 'ユーザーの配送先住所。複数住所登録可能';

-- ----------------------------------------------------------------------------
-- user_sessions: セッション管理
-- ----------------------------------------------------------------------------
CREATE TABLE user_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    access_token_hash VARCHAR(255) NOT NULL, -- JWTまたはセッショントークンのハッシュ
    refresh_token_hash VARCHAR(255), -- リフレッシュトークンのハッシュ
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_activity_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_access_token ON user_sessions(access_token_hash);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at) WHERE revoked_at IS NULL;

COMMENT ON TABLE user_sessions IS 'アクティブなユーザーセッション。アクセストークン・リフレッシュトークンの管理';
COMMENT ON COLUMN user_sessions.access_token_hash IS 'アクセストークンのハッシュ値（トークン自体は保存しない）';
COMMENT ON COLUMN user_sessions.expires_at IS 'セッション有効期限（アクセストークン短寿命: 15分想定）';

-- ----------------------------------------------------------------------------
-- user_auth_events: 認証イベント監査ログ
-- ----------------------------------------------------------------------------
CREATE TABLE user_auth_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    event_type VARCHAR(50) NOT NULL, -- LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, PASSWORD_CHANGE, MFA_ENABLED, TOKEN_ISSUED, TOKEN_REVOKED
    email VARCHAR(255), -- ログイン失敗時などuser_idがない場合に記録
    ip_address VARCHAR(45),
    user_agent TEXT,
    event_metadata JSONB, -- 追加情報（失敗理由、変更前後の値など）
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- パーティション作成例（月次）
CREATE TABLE user_auth_events_2024_11 PARTITION OF user_auth_events
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_user_auth_events_user_id ON user_auth_events(user_id, created_at);
CREATE INDEX idx_user_auth_events_type ON user_auth_events(event_type, created_at);
CREATE INDEX idx_user_auth_events_ip ON user_auth_events(ip_address, created_at);

COMMENT ON TABLE user_auth_events IS '認証関連イベントの監査ログ。最低1年保持';
COMMENT ON COLUMN user_auth_events.event_metadata IS 'イベント固有の追加情報（JSON形式）';

-- ----------------------------------------------------------------------------
-- password_reset_tokens: パスワードリセットトークン
-- ----------------------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    token_hash VARCHAR(255) NOT NULL UNIQUE, -- トークンのハッシュ値
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires ON password_reset_tokens(expires_at) WHERE used_at IS NULL;

COMMENT ON TABLE password_reset_tokens IS 'パスワードリセット用ワンタイムトークン（有効期限: 1時間程度）';

-- ----------------------------------------------------------------------------
-- email_verification_tokens: メール確認トークン
-- ----------------------------------------------------------------------------
CREATE TABLE email_verification_tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);

COMMENT ON TABLE email_verification_tokens IS 'メールアドレス確認用トークン';
