-- ============================================================================
-- 注文ドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- carts: カート
-- ----------------------------------------------------------------------------
CREATE TABLE carts (
    cart_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    session_id VARCHAR(255), -- 未ログインユーザー用のセッションID
    cart_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ABANDONED, CONVERTED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, -- カート有効期限
    converted_order_id UUID -- 注文確定時に設定
);

CREATE INDEX idx_carts_user_id ON carts(user_id);
CREATE INDEX idx_carts_session_id ON carts(session_id);
CREATE INDEX idx_carts_status ON carts(cart_status);
CREATE INDEX idx_carts_expires ON carts(expires_at) WHERE cart_status = 'ACTIVE';

COMMENT ON TABLE carts IS 'ショッピングカート。ログイン前後で統合可能';
COMMENT ON COLUMN carts.expires_at IS 'カート有効期限（通常24-48時間）';

-- ----------------------------------------------------------------------------
-- cart_items: カート明細
-- ----------------------------------------------------------------------------
CREATE TABLE cart_items (
    cart_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(cart_id),
    sku_id UUID NOT NULL REFERENCES skus(sku_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_sku_id ON cart_items(sku_id);

COMMENT ON TABLE cart_items IS 'カート明細。SKUと数量を保持';

-- ----------------------------------------------------------------------------
-- orders: 注文
-- ----------------------------------------------------------------------------
CREATE TABLE orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) NOT NULL UNIQUE, -- 注文番号（業務キー、顧客向け表示用）
    user_id UUID NOT NULL REFERENCES users(user_id),
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, ALLOCATED, SHIPPED, DELIVERED, COMPLETED, CANCELLED, FAILED
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED
    
    -- 金額情報
    subtotal_amount DECIMAL(12, 2) NOT NULL, -- 商品小計
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0, -- 割引額
    shipping_fee DECIMAL(10, 2) NOT NULL DEFAULT 0, -- 送料
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0, -- 消費税
    total_amount DECIMAL(12, 2) NOT NULL, -- 合計金額
    currency VARCHAR(3) NOT NULL DEFAULT 'JPY',
    
    -- 配送先情報
    shipping_address_id UUID REFERENCES user_addresses(address_id),
    recipient_name VARCHAR(200) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_postal_code VARCHAR(10) NOT NULL,
    shipping_prefecture VARCHAR(50) NOT NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_address_line1 VARCHAR(255) NOT NULL,
    shipping_address_line2 VARCHAR(255),
    
    -- メタデータ
    customer_note TEXT,
    is_gift BOOLEAN NOT NULL DEFAULT FALSE,
    gift_message TEXT,
    ordered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1 -- 楽観ロック用
) PARTITION BY RANGE (ordered_at);

-- パーティション作成例（月次）
CREATE TABLE orders_2024_11 PARTITION OF orders
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_orders_user_id ON orders(user_id, ordered_at);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(order_status, ordered_at);
CREATE INDEX idx_orders_payment_status ON orders(payment_status);

COMMENT ON TABLE orders IS '注文ヘッダー。注文全体の情報を管理';
COMMENT ON COLUMN orders.order_number IS '顧客向け注文番号（例: ORD-20241101-12345）';
COMMENT ON COLUMN orders.confirmed_at IS '注文確定日時（在庫引当完了時点）';

-- ----------------------------------------------------------------------------
-- order_lines: 注文明細
-- ----------------------------------------------------------------------------
CREATE TABLE order_lines (
    order_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    line_number INTEGER NOT NULL, -- 明細番号（1, 2, 3...）
    sku_id UUID NOT NULL REFERENCES skus(sku_id),
    product_name VARCHAR(255) NOT NULL, -- 注文時のスナップショット
    sku_code VARCHAR(100) NOT NULL,
    color VARCHAR(50),
    size VARCHAR(20),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    line_total DECIMAL(10, 2) NOT NULL,
    allocated_location_id UUID REFERENCES inventory_locations(location_id),
    allocated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(order_id, line_number)
);

CREATE INDEX idx_order_lines_order_id ON order_lines(order_id);
CREATE INDEX idx_order_lines_sku_id ON order_lines(sku_id);
CREATE INDEX idx_order_lines_allocated_location ON order_lines(allocated_location_id);

COMMENT ON TABLE order_lines IS '注文明細。SKUごとの購入内容を保持';
COMMENT ON COLUMN order_lines.allocated_location_id IS '在庫引当された拠点ID';

-- ----------------------------------------------------------------------------
-- order_status_history: 注文ステータス履歴
-- ----------------------------------------------------------------------------
CREATE TABLE order_status_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_reason TEXT,
    changed_by UUID REFERENCES users(user_id),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (changed_at);

-- パーティション作成例（月次）
CREATE TABLE order_status_history_2024_11 PARTITION OF order_status_history
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id, changed_at);
CREATE INDEX idx_order_status_history_status ON order_status_history(to_status, changed_at);

COMMENT ON TABLE order_status_history IS '注文ステータス変更履歴。監査とトラッキングに使用';
