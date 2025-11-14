-- ============================================================================
-- 出荷・物流ドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- shipments: 出荷
-- ----------------------------------------------------------------------------
CREATE TABLE shipments (
    shipment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_number VARCHAR(50) NOT NULL UNIQUE, -- 出荷番号（業務キー）
    order_id UUID NOT NULL,
    location_id UUID NOT NULL REFERENCES inventory_locations(location_id),
    carrier_code VARCHAR(50) NOT NULL, -- 配送業者コード（YAMATO, SAGAWA, JP_POST等）
    carrier_name VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(100), -- 配送追跡番号
    
    shipment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, PICKED, PACKED, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED
    
    -- 配送先情報（スナップショット）
    recipient_name VARCHAR(200) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_postal_code VARCHAR(10) NOT NULL,
    shipping_prefecture VARCHAR(50) NOT NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_address_line1 VARCHAR(255) NOT NULL,
    shipping_address_line2 VARCHAR(255),
    
    -- 日時情報
    scheduled_ship_date DATE,
    estimated_delivery_date DATE,
    actual_ship_date DATE,
    actual_delivery_date DATE,
    
    -- 3PL連携情報
    external_shipment_id VARCHAR(100), -- 3PLシステムの出荷ID
    picking_list_generated_at TIMESTAMP WITH TIME ZONE,
    label_generated_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- パーティション作成例（月次）
CREATE TABLE shipments_2024_11 PARTITION OF shipments
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_shipments_order_id ON shipments(order_id);
CREATE INDEX idx_shipments_location_id ON shipments(location_id);
CREATE INDEX idx_shipments_tracking ON shipments(tracking_number);
CREATE INDEX idx_shipments_status ON shipments(shipment_status, created_at);
CREATE INDEX idx_shipments_carrier ON shipments(carrier_code);

COMMENT ON TABLE shipments IS '出荷情報。3PL連携と配送追跡を管理';
COMMENT ON COLUMN shipments.tracking_number IS '配送業者の追跡番号';
COMMENT ON COLUMN shipments.external_shipment_id IS '外部3PLシステムの出荷ID';

-- ----------------------------------------------------------------------------
-- shipment_items: 出荷明細
-- ----------------------------------------------------------------------------
CREATE TABLE shipment_items (
    shipment_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL,
    order_line_id UUID NOT NULL,
    sku_id UUID NOT NULL REFERENCES skus(sku_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shipment_items_shipment_id ON shipment_items(shipment_id);
CREATE INDEX idx_shipment_items_order_line_id ON shipment_items(order_line_id);

COMMENT ON TABLE shipment_items IS '出荷明細。注文明細と紐づけて出荷内容を管理';

-- ----------------------------------------------------------------------------
-- shipment_trackings: 配送追跡
-- ----------------------------------------------------------------------------
CREATE TABLE shipment_trackings (
    tracking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL,
    tracking_status VARCHAR(50) NOT NULL, -- LABEL_CREATED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, EXCEPTION
    tracking_location VARCHAR(255), -- 現在地
    tracking_message TEXT, -- 配送業者からのメッセージ
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- パーティション作成例（月次）
CREATE TABLE shipment_trackings_2024_11 PARTITION OF shipment_trackings
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_shipment_trackings_shipment_id ON shipment_trackings(shipment_id, event_timestamp);
CREATE INDEX idx_shipment_trackings_status ON shipment_trackings(tracking_status, created_at);

COMMENT ON TABLE shipment_trackings IS '配送追跡イベント。配送業者APIから取得した情報を記録';
COMMENT ON COLUMN shipment_trackings.event_timestamp IS '配送業者が記録したイベント発生日時';
