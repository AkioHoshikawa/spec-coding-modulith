-- ============================================================================
-- 監査ログドメイン DDL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- audit_logs: 操作監査ログ
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    entity_type VARCHAR(50) NOT NULL, -- ORDER, PRODUCT, INVENTORY, USER, PAYMENT, etc.
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, APPROVE, CANCEL, etc.
    
    -- 変更内容
    old_values JSONB, -- 変更前の値
    new_values JSONB, -- 変更後の値
    
    -- メタデータ
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id UUID, -- リクエストトレーシング用ID
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- パーティション作成例（月次）
CREATE TABLE audit_logs_2024_11 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id, created_at);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at);
CREATE INDEX idx_audit_logs_action ON audit_logs(action, created_at);
CREATE INDEX idx_audit_logs_request_id ON audit_logs(request_id);

COMMENT ON TABLE audit_logs IS '操作監査ログ。すべての重要操作を記録。最低1年保持';
COMMENT ON COLUMN audit_logs.old_values IS '変更前の値（JSON形式）';
COMMENT ON COLUMN audit_logs.new_values IS '変更後の値（JSON形式）';

-- ----------------------------------------------------------------------------
-- system_events: システムイベントログ
-- ----------------------------------------------------------------------------
CREATE TABLE system_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL, -- INVENTORY_NEGATIVE, SHIPMENT_DELAYED, PAYMENT_FAILED, API_ERROR, etc.
    severity VARCHAR(20) NOT NULL, -- INFO, WARNING, ERROR, CRITICAL
    event_source VARCHAR(50) NOT NULL, -- SERVICE_NAME or MODULE_NAME
    
    -- イベント詳細
    event_title VARCHAR(255) NOT NULL,
    event_message TEXT,
    event_data JSONB, -- イベント固有のデータ
    
    -- 関連エンティティ
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    
    -- アラート管理
    alert_sent BOOLEAN NOT NULL DEFAULT FALSE,
    alert_sent_at TIMESTAMP WITH TIME ZONE,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by UUID REFERENCES users(user_id),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- パーティション作成例（月次）
CREATE TABLE system_events_2024_11 PARTITION OF system_events
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE INDEX idx_system_events_type ON system_events(event_type, created_at);
CREATE INDEX idx_system_events_severity ON system_events(severity, created_at);
CREATE INDEX idx_system_events_source ON system_events(event_source, created_at);
CREATE INDEX idx_system_events_related_entity ON system_events(related_entity_type, related_entity_id);
CREATE INDEX idx_system_events_unacknowledged ON system_events(acknowledged, severity) WHERE acknowledged = FALSE;

COMMENT ON TABLE system_events IS 'システムイベント・アラートログ。異常検知と運用監視に使用';
COMMENT ON COLUMN system_events.severity IS 'イベントの重要度（INFO/WARNING/ERROR/CRITICAL）';
COMMENT ON COLUMN system_events.alert_sent IS 'アラート通知済みフラグ';
