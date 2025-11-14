-- ============================================================================
-- 決済ドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- payments: 支払い
-- ----------------------------------------------------------------------------
CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    payment_method VARCHAR(30) NOT NULL, -- CREDIT_CARD, BANK_TRANSFER, COD, E_MONEY, PAY_LATER
    payment_provider VARCHAR(50), -- STRIPE, PAYPAY, GMO, etc.
    
    -- 金額情報
    payment_amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'JPY',
    
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, AUTHORIZED, CAPTURED, FAILED, CANCELLED, REFUNDED, PARTIAL_REFUNDED
    
    -- 外部決済情報
    external_payment_id VARCHAR(100), -- 決済プロバイダの決済ID
    payment_token VARCHAR(255), -- トークン化されたカード情報
    card_last4 VARCHAR(4), -- カード下4桁（表示用）
    card_brand VARCHAR(20), -- VISA, MASTERCARD, JCB, etc.
    
    -- 認可・確定情報
    authorized_amount DECIMAL(12, 2),
    authorized_at TIMESTAMP WITH TIME ZONE,
    captured_amount DECIMAL(12, 2),
    captured_at TIMESTAMP WITH TIME ZONE,
    
    -- 返金情報
    refunded_amount DECIMAL(12, 2) DEFAULT 0,
    
    -- エラー情報
    error_code VARCHAR(50),
    error_message TEXT,
    
    -- メタデータ
    payment_metadata JSONB, -- 決済プロバイダからの追加情報
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- パーティション作成例（月次）
CREATE TABLE payments_2024_11 PARTITION OF payments
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(payment_status, created_at);
CREATE INDEX idx_payments_external_id ON payments(external_payment_id);
CREATE INDEX idx_payments_provider ON payments(payment_provider);

COMMENT ON TABLE payments IS '支払い情報。決済プロバイダとの連携を管理';
COMMENT ON COLUMN payments.payment_token IS 'トークン化されたカード情報（PCI-DSS準拠）';
COMMENT ON COLUMN payments.external_payment_id IS '決済プロバイダ（Stripe等）の決済ID';

-- ----------------------------------------------------------------------------
-- payment_transactions: 決済トランザクション
-- ----------------------------------------------------------------------------
CREATE TABLE payment_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL, -- AUTHORIZE, CAPTURE, REFUND, VOID, CHARGEBACK
    transaction_amount DECIMAL(12, 2) NOT NULL,
    transaction_status VARCHAR(30) NOT NULL, -- PENDING, SUCCESS, FAILED
    
    external_transaction_id VARCHAR(100), -- 決済プロバイダのトランザクションID
    
    -- レスポンス情報
    response_code VARCHAR(50),
    response_message TEXT,
    
    -- メタデータ
    transaction_metadata JSONB,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
) PARTITION BY RANGE (created_at);

-- パーティション作成例（月次）
CREATE TABLE payment_transactions_2024_11 PARTITION OF payment_transactions
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions(payment_id, created_at);
CREATE INDEX idx_payment_transactions_type ON payment_transactions(transaction_type, created_at);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(transaction_status);

COMMENT ON TABLE payment_transactions IS '決済トランザクション履歴。すべての決済操作を記録';
COMMENT ON COLUMN payment_transactions.transaction_type IS '操作種別（認可・確定・返金等）';
