-- ============================================================================
-- プロモーションドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- coupons: クーポン
-- ----------------------------------------------------------------------------
CREATE TABLE coupons (
    coupon_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_code VARCHAR(50) NOT NULL UNIQUE,
    
    -- 割引設定（定率割引のみ）
    discount_percentage INTEGER NOT NULL CHECK (discount_percentage > 0 AND discount_percentage <= 100),
    
    -- 使用制限
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    
    coupon_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coupons_code ON coupons(coupon_code);
CREATE INDEX idx_coupons_status ON coupons(coupon_status);
CREATE INDEX idx_coupons_valid ON coupons(valid_from, valid_until) WHERE coupon_status = 'ACTIVE';

COMMENT ON TABLE coupons IS 'クーポンマスタ。簡易的な定率割引クーポンを管理';
