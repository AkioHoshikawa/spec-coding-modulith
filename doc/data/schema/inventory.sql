-- ============================================================================
-- 商品・在庫ドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- products: 商品マスタ
-- ----------------------------------------------------------------------------
CREATE TABLE products (
    product_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code VARCHAR(50) NOT NULL UNIQUE, -- 商品コード（業務キー）
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    brand VARCHAR(100),
    category VARCHAR(100) NOT NULL, -- TOPS, BOTTOMS, OUTERWEAR, ACCESSORIES, etc.
    product_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISCONTINUED, DRAFT
    base_price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'JPY',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_products_code ON products(product_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_category ON products(category) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_status ON products(product_status) WHERE deleted_at IS NULL;

COMMENT ON TABLE products IS '商品マスタ。商品の基本情報を管理';

-- ----------------------------------------------------------------------------
-- skus: SKU（Stock Keeping Unit）
-- ----------------------------------------------------------------------------
CREATE TABLE skus (
    sku_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(product_id),
    sku_code VARCHAR(100) NOT NULL UNIQUE, -- SKUコード（業務キー）
    color VARCHAR(50),
    size VARCHAR(20),
    material VARCHAR(100),
    sku_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISCONTINUED
    retail_price DECIMAL(10, 2) NOT NULL,
    cost_price DECIMAL(10, 2),
    weight_grams INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_skus_product_id ON skus(product_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_skus_code ON skus(sku_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_skus_status ON skus(sku_status) WHERE deleted_at IS NULL;

COMMENT ON TABLE skus IS 'SKU（在庫管理単位）。カラー・サイズ展開ごとに管理';
COMMENT ON COLUMN skus.sku_code IS '業務上の一意なSKUコード';

-- ----------------------------------------------------------------------------
-- inventory: 在庫数
-- ----------------------------------------------------------------------------
CREATE TABLE inventory (
    sku_id UUID PRIMARY KEY REFERENCES skus(sku_id),
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0), -- 利用可能在庫数
    version INTEGER NOT NULL DEFAULT 1, -- 楽観ロック用
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_quantity ON inventory(quantity) WHERE quantity > 0;

COMMENT ON TABLE inventory IS 'SKUごとの在庫数。コンカレンシー対策で楽観ロック使用';
COMMENT ON COLUMN inventory.quantity IS '販売可能な在庫数。負数禁止';
COMMENT ON COLUMN inventory.version IS '楽観ロック用バージョン（在庫競合検出）';

-- ----------------------------------------------------------------------------
-- inventory_transactions: 在庫トランザクション履歴
-- ----------------------------------------------------------------------------
CREATE TABLE inventory_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id UUID NOT NULL REFERENCES skus(sku_id),
    transaction_type VARCHAR(30) NOT NULL, -- RECEIVE, SHIP, ADJUST, LOCK, UNLOCK
    quantity_change INTEGER NOT NULL, -- 増減量（+/-）
    quantity_before INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    reference_type VARCHAR(50), -- ORDER, MANUAL
    reference_id UUID, -- 関連エンティティのID
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(user_id)
) PARTITION BY RANGE (created_at);

-- パーティション作成例（月次）
CREATE TABLE inventory_transactions_2024_11 PARTITION OF inventory_transactions
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_inventory_transactions_sku ON inventory_transactions(sku_id, created_at);
CREATE INDEX idx_inventory_transactions_reference ON inventory_transactions(reference_type, reference_id);

COMMENT ON TABLE inventory_transactions IS '在庫トランザクション履歴。すべての在庫増減を記録';
COMMENT ON COLUMN inventory_transactions.quantity_change IS '在庫増減量（入庫: +、出庫: -）';
COMMENT ON COLUMN inventory_transactions.reference_id IS '関連する注文等のID';
