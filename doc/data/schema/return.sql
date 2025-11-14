-- ============================================================================
-- 返品・交換ドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- returns: 返品
-- ----------------------------------------------------------------------------
CREATE TABLE returns (
    return_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_number VARCHAR(50) NOT NULL UNIQUE, -- 返品番号（業務キー）
    order_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id),
    
    return_type VARCHAR(30) NOT NULL, -- REFUND, EXCHANGE, STORE_CREDIT
    return_reason VARCHAR(50) NOT NULL, -- SIZE_ISSUE, DEFECT, NOT_AS_EXPECTED, CHANGE_OF_MIND, WRONG_ITEM
    return_reason_detail TEXT,
    
    return_status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED', -- REQUESTED, APPROVED, REJECTED, RECEIVED, INSPECTED, REFUNDED, COMPLETED, CANCELLED
    
    -- 返品金額
    refund_amount DECIMAL(10, 2),
    restocking_fee DECIMAL(10, 2) DEFAULT 0,
    return_shipping_fee DECIMAL(10, 2) DEFAULT 0,
    
    -- 返送情報
    return_shipping_carrier VARCHAR(50),
    return_tracking_number VARCHAR(100),
    
    -- 日時情報
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    received_at TIMESTAMP WITH TIME ZONE,
    inspected_at TIMESTAMP WITH TIME ZONE,
    refunded_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    -- メタデータ
    customer_note TEXT,
    internal_note TEXT,
    images JSONB, -- 返品商品の写真URL配列
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(user_id),
    approved_by UUID REFERENCES users(user_id)
) PARTITION BY RANGE (requested_at);

-- パーティション作成例（月次）
CREATE TABLE returns_2024_11 PARTITION OF returns
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_returns_order_id ON returns(order_id);
CREATE INDEX idx_returns_user_id ON returns(user_id, requested_at);
CREATE INDEX idx_returns_status ON returns(return_status, requested_at);
CREATE INDEX idx_returns_number ON returns(return_number);

COMMENT ON TABLE returns IS '返品情報。返品申請から返金完了までを管理';
COMMENT ON COLUMN returns.return_type IS '返品タイプ（返金・交換・ストアクレジット）';
COMMENT ON COLUMN returns.images IS '返品商品の写真URL（JSON配列）';

-- ----------------------------------------------------------------------------
-- return_items: 返品明細
-- ----------------------------------------------------------------------------
CREATE TABLE return_items (
    return_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_id UUID NOT NULL,
    order_line_id UUID NOT NULL,
    sku_id UUID NOT NULL REFERENCES skus(sku_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_refund_amount DECIMAL(10, 2) NOT NULL,
    item_condition VARCHAR(30), -- NEW, USED, DAMAGED
    inspection_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_return_items_return_id ON return_items(return_id);
CREATE INDEX idx_return_items_order_line_id ON return_items(order_line_id);

COMMENT ON TABLE return_items IS '返品明細。返品対象の商品情報を管理';
COMMENT ON COLUMN return_items.item_condition IS '返品商品の状態（検品結果）';

-- ----------------------------------------------------------------------------
-- exchanges: 交換
-- ----------------------------------------------------------------------------
CREATE TABLE exchanges (
    exchange_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exchange_number VARCHAR(50) NOT NULL UNIQUE,
    return_id UUID NOT NULL REFERENCES returns(return_id),
    original_order_id UUID NOT NULL,
    new_order_id UUID, -- 交換による新規注文ID
    
    exchange_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, PROCESSING, SHIPPED, COMPLETED, CANCELLED
    
    exchange_reason VARCHAR(50) NOT NULL, -- SIZE_CHANGE, COLOR_CHANGE, DEFECT_REPLACEMENT
    additional_payment_amount DECIMAL(10, 2) DEFAULT 0, -- 差額支払い（新商品が高い場合）
    refund_difference_amount DECIMAL(10, 2) DEFAULT 0, -- 差額返金（新商品が安い場合）
    
    approved_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(user_id)
);

CREATE INDEX idx_exchanges_return_id ON exchanges(return_id);
CREATE INDEX idx_exchanges_original_order_id ON exchanges(original_order_id);
CREATE INDEX idx_exchanges_new_order_id ON exchanges(new_order_id);
CREATE INDEX idx_exchanges_status ON exchanges(exchange_status);

COMMENT ON TABLE exchanges IS '交換情報。返品と新規注文を紐づけて管理';
COMMENT ON COLUMN exchanges.new_order_id IS '交換で作成された新規注文のID';

-- ----------------------------------------------------------------------------
-- exchange_items: 交換明細
-- ----------------------------------------------------------------------------
CREATE TABLE exchange_items (
    exchange_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exchange_id UUID NOT NULL REFERENCES exchanges(exchange_id),
    return_item_id UUID NOT NULL REFERENCES return_items(return_item_id),
    new_sku_id UUID NOT NULL REFERENCES skus(sku_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exchange_items_exchange_id ON exchange_items(exchange_id);
CREATE INDEX idx_exchange_items_return_item_id ON exchange_items(return_item_id);

COMMENT ON TABLE exchange_items IS '交換明細。返品商品と交換先商品を紐づけ';
