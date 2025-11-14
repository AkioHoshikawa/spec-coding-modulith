-- ============================================================================
-- プロモーションドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- promotions: プロモーション
-- ----------------------------------------------------------------------------
CREATE TABLE promotions (
    promotion_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_code VARCHAR(50) NOT NULL UNIQUE,
    promotion_name VARCHAR(255) NOT NULL,
    promotion_type VARCHAR(30) NOT NULL, -- PERCENTAGE_DISCOUNT, FIXED_DISCOUNT, BUY_X_GET_Y, TIME_SALE, PRE_ORDER
    description TEXT,
    discount_percentage DECIMAL(5, 2), -- 割引率（例: 20.00 = 20%）
    discount_amount DECIMAL(10, 2), -- 固定割引額
    minimum_purchase_amount DECIMAL(10, 2), -- 最小購入金額
    max_discount_amount DECIMAL(10, 2), -- 最大割引額
    
    -- 適用範囲
    applicable_products JSONB, -- 対象商品ID配列
    applicable_categories JSONB, -- 対象カテゴリ配列
    
    -- 期間設定
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- 使用制限
    usage_limit INTEGER, -- 全体使用上限
    usage_limit_per_user INTEGER, -- ユーザーごとの使用上限
    current_usage_count INTEGER NOT NULL DEFAULT 0,
    
    -- 在庫制約
    inventory_limited BOOLEAN NOT NULL DEFAULT FALSE,
    reserved_inventory_quantity INTEGER,
    
    -- ステータス
    promotion_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, PAUSED, EXPIRED, CANCELLED
    priority INTEGER NOT NULL DEFAULT 0, -- 複数プロモーション適用時の優先度
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(user_id)
);

CREATE INDEX idx_promotions_code ON promotions(promotion_code);
CREATE INDEX idx_promotions_status ON promotions(promotion_status);
CREATE INDEX idx_promotions_period ON promotions(start_at, end_at) WHERE promotion_status = 'ACTIVE';
CREATE INDEX idx_promotions_type ON promotions(promotion_type);

COMMENT ON TABLE promotions IS 'プロモーション定義。セール、タイムセール、予約販売等を管理';
COMMENT ON COLUMN promotions.priority IS '複数プロモーション適用時の優先度（高い値が優先）';
COMMENT ON COLUMN promotions.inventory_limited IS '在庫制約付きプロモーションか';

-- ----------------------------------------------------------------------------
-- promotion_rules: プロモーションルール
-- ----------------------------------------------------------------------------
CREATE TABLE promotion_rules (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID NOT NULL REFERENCES promotions(promotion_id),
    rule_type VARCHAR(30) NOT NULL, -- MIN_QUANTITY, MIN_AMOUNT, SPECIFIC_SKU, USER_SEGMENT
    rule_condition JSONB NOT NULL, -- ルール条件（JSON形式）
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_promotion_rules_promotion_id ON promotion_rules(promotion_id);

COMMENT ON TABLE promotion_rules IS 'プロモーション適用ルール。複雑な条件設定に対応';
COMMENT ON COLUMN promotion_rules.rule_condition IS 'ルール条件の詳細（JSON形式）';

-- ----------------------------------------------------------------------------
-- coupons: クーポン
-- ----------------------------------------------------------------------------
CREATE TABLE coupons (
    coupon_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID REFERENCES promotions(promotion_id),
    coupon_code VARCHAR(50) NOT NULL UNIQUE,
    coupon_type VARCHAR(30) NOT NULL, -- SINGLE_USE, MULTI_USE, USER_SPECIFIC
    
    -- 割引設定
    discount_percentage DECIMAL(5, 2),
    discount_amount DECIMAL(10, 2),
    max_discount_amount DECIMAL(10, 2),
    minimum_purchase_amount DECIMAL(10, 2),
    
    -- 使用制限
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    usage_limit INTEGER,
    current_usage_count INTEGER NOT NULL DEFAULT 0,
    
    -- ユーザー紐付け
    assigned_user_id UUID REFERENCES users(user_id), -- ユーザー固有クーポンの場合
    
    coupon_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, USED, EXPIRED, CANCELLED
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coupons_code ON coupons(coupon_code);
CREATE INDEX idx_coupons_promotion_id ON coupons(promotion_id);
CREATE INDEX idx_coupons_user_id ON coupons(assigned_user_id);
CREATE INDEX idx_coupons_status ON coupons(coupon_status);
CREATE INDEX idx_coupons_valid ON coupons(valid_from, valid_until) WHERE coupon_status = 'ACTIVE';

COMMENT ON TABLE coupons IS 'クーポンマスタ。プロモーションとは別に個別クーポンを管理';
COMMENT ON COLUMN coupons.assigned_user_id IS 'ユーザー固有クーポンの場合の対象ユーザーID';

-- ----------------------------------------------------------------------------
-- coupon_usages: クーポン使用履歴
-- ----------------------------------------------------------------------------
CREATE TABLE coupon_usages (
    usage_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id UUID NOT NULL REFERENCES coupons(coupon_id),
    order_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id),
    discount_amount DECIMAL(10, 2) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (used_at);

-- パーティション作成例（月次）
CREATE TABLE coupon_usages_2024_11 PARTITION OF coupon_usages
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_coupon_usages_coupon_id ON coupon_usages(coupon_id, used_at);
CREATE INDEX idx_coupon_usages_order_id ON coupon_usages(order_id);
CREATE INDEX idx_coupon_usages_user_id ON coupon_usages(user_id, used_at);

COMMENT ON TABLE coupon_usages IS 'クーポン使用履歴。監査とレポートに使用';
