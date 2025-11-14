-- ============================================================================
-- インデックス管理・パフォーマンスチューニング DDL
-- ============================================================================

-- このファイルは全テーブルの追加インデックス、パーティション管理、
-- パフォーマンス最適化のためのSQL定義をまとめています。

-- ============================================================================
-- パーティション管理の自動化
-- ============================================================================

-- パーティション自動作成関数（月次パーティション）
CREATE OR REPLACE FUNCTION create_monthly_partitions(
    table_name TEXT,
    months_ahead INTEGER DEFAULT 3
) RETURNS void AS $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
    i INTEGER;
BEGIN
    FOR i IN 0..months_ahead LOOP
        start_date := DATE_TRUNC('month', CURRENT_DATE + (i || ' months')::INTERVAL);
        end_date := start_date + INTERVAL '1 month';
        partition_name := table_name || '_' || TO_CHAR(start_date, 'YYYY_MM');
        
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
            partition_name, table_name, start_date, end_date
        );
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- パーティション自動作成のスケジュール設定例（pg_cronなどで実行）
-- SELECT create_monthly_partitions('orders', 3);
-- SELECT create_monthly_partitions('order_status_history', 3);
-- SELECT create_monthly_partitions('inventory_transactions', 3);
-- SELECT create_monthly_partitions('user_auth_events', 3);
-- SELECT create_monthly_partitions('audit_logs', 3);
-- SELECT create_monthly_partitions('system_events', 3);
-- SELECT create_monthly_partitions('payments', 3);
-- SELECT create_monthly_partitions('payment_transactions', 3);
-- SELECT create_monthly_partitions('shipments', 3);
-- SELECT create_monthly_partitions('shipment_trackings', 3);
-- SELECT create_monthly_partitions('returns', 3);
-- SELECT create_monthly_partitions('coupon_usages', 3);

-- ============================================================================
-- 複合インデックス（クエリ最適化）
-- ============================================================================

-- 注文検索の最適化
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_user_status_date 
ON orders(user_id, order_status, ordered_at DESC) 
WHERE deleted_at IS NULL;

-- 在庫検索の最適化
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inventory_stocks_sku_available 
ON inventory_stocks(sku_id, available_quantity DESC) 
WHERE available_quantity > 0;

-- 出荷ステータス検索の最適化
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_shipments_status_date 
ON shipments(shipment_status, scheduled_ship_date) 
WHERE shipment_status IN ('PENDING', 'PICKED', 'PACKED');

-- 返品ステータス検索の最適化
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_returns_status_requested 
ON returns(return_status, requested_at DESC) 
WHERE return_status IN ('REQUESTED', 'APPROVED', 'RECEIVED');

-- ============================================================================
-- カバリングインデックス（頻繁なクエリ用）
-- ============================================================================

-- 注文一覧表示用（管理画面）
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_list_covering 
ON orders(ordered_at DESC, order_status) 
INCLUDE (order_number, user_id, total_amount, payment_status);

-- ユーザー注文履歴用
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_user_history 
ON orders(user_id, ordered_at DESC) 
INCLUDE (order_number, total_amount, order_status);

-- ============================================================================
-- 部分インデックス（条件付きインデックス）
-- ============================================================================

-- アクティブプロモーションのみ
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_promotions_active 
ON promotions(start_at, end_at) 
WHERE promotion_status = 'ACTIVE' AND deleted_at IS NULL;

-- 有効なクーポンのみ
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_valid 
ON coupons(coupon_code, valid_from, valid_until) 
WHERE coupon_status = 'ACTIVE';

-- 処理中の注文のみ
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_processing 
ON orders(ordered_at DESC) 
WHERE order_status IN ('PENDING', 'CONFIRMED', 'ALLOCATED');

-- アクティブな在庫ロックのみ
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inventory_locks_active 
ON inventory_locks(sku_id, location_id, expires_at) 
WHERE released_at IS NULL;

-- ============================================================================
-- 全文検索インデックス（商品検索）
-- ============================================================================

-- 商品名の全文検索用
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_name_fts 
ON products USING gin(to_tsvector('japanese', product_name));

-- 商品説明の全文検索用
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_products_description_fts 
ON products USING gin(to_tsvector('japanese', description));

-- ============================================================================
-- JSONB インデックス（メタデータ検索）
-- ============================================================================

-- プロモーション適用商品の検索
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_promotions_applicable_products 
ON promotions USING gin(applicable_products);

-- システムイベントデータの検索
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_system_events_data 
ON system_events USING gin(event_data);

-- 監査ログの変更値検索
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_values 
ON audit_logs USING gin(new_values);

-- ============================================================================
-- 統計情報の更新
-- ============================================================================

-- 定期的に統計情報を更新する関数
CREATE OR REPLACE FUNCTION update_table_statistics() RETURNS void AS $$
BEGIN
    -- 主要テーブルの統計情報更新
    ANALYZE users;
    ANALYZE products;
    ANALYZE skus;
    ANALYZE inventory_stocks;
    ANALYZE orders;
    ANALYZE order_lines;
    ANALYZE shipments;
    ANALYZE payments;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- インデックスメンテナンス
-- ============================================================================

-- 肥大化したインデックスの再構築関数
CREATE OR REPLACE FUNCTION reindex_bloated_indexes(bloat_threshold FLOAT DEFAULT 0.3) 
RETURNS TABLE(index_name TEXT, bloat_ratio FLOAT) AS $$
DECLARE
    idx RECORD;
BEGIN
    -- 肥大化インデックスの検出と再構築
    FOR idx IN 
        SELECT indexrelname, 
               pg_relation_size(indexrelid)::FLOAT / NULLIF(pg_relation_size(relid), 0) as ratio
        FROM pg_stat_user_indexes
        WHERE pg_relation_size(indexrelid) > 100 * 1024 * 1024 -- 100MB以上
    LOOP
        IF idx.ratio > bloat_threshold THEN
            EXECUTE 'REINDEX INDEX CONCURRENTLY ' || idx.indexrelname;
            index_name := idx.indexrelname;
            bloat_ratio := idx.ratio;
            RETURN NEXT;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- クエリパフォーマンス監視ビュー
-- ============================================================================

-- スロークエリ検出ビュー
CREATE OR REPLACE VIEW slow_queries AS
SELECT 
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    rows / calls AS avg_rows
FROM pg_stat_statements
WHERE mean_exec_time > 1000 -- 1秒以上
ORDER BY mean_exec_time DESC
LIMIT 50;

-- テーブルサイズ監視ビュー
CREATE OR REPLACE VIEW table_sizes AS
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS index_size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- インデックス使用率監視ビュー
CREATE OR REPLACE VIEW index_usage AS
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC, pg_relation_size(indexrelid) DESC;

-- ============================================================================
-- パーティション削除（古いデータのクリーンアップ）
-- ============================================================================

-- 古いパーティションを削除する関数
CREATE OR REPLACE FUNCTION drop_old_partitions(
    table_name TEXT,
    keep_months INTEGER DEFAULT 12
) RETURNS void AS $$
DECLARE
    partition_record RECORD;
    cutoff_date DATE;
BEGIN
    cutoff_date := DATE_TRUNC('month', CURRENT_DATE - (keep_months || ' months')::INTERVAL);
    
    FOR partition_record IN 
        SELECT inhrelid::regclass AS partition_name
        FROM pg_inherits
        WHERE inhparent = table_name::regclass
    LOOP
        -- パーティション名から日付を抽出して判定
        -- 例: orders_2023_01 -> 2023-01-01
        IF partition_record.partition_name::text ~ '_\d{4}_\d{2}$' THEN
            DECLARE
                partition_date DATE;
            BEGIN
                partition_date := TO_DATE(
                    regexp_replace(partition_record.partition_name::text, '.*_(\d{4})_(\d{2})$', '\1-\2-01'),
                    'YYYY-MM-DD'
                );
                
                IF partition_date < cutoff_date THEN
                    RAISE NOTICE 'Dropping old partition: %', partition_record.partition_name;
                    EXECUTE 'DROP TABLE IF EXISTS ' || partition_record.partition_name;
                END IF;
            END;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- バキューム設定の最適化
-- ============================================================================

-- 高頻度更新テーブルのバキューム設定
ALTER TABLE inventory_stocks SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_vacuum_cost_delay = 10
);

ALTER TABLE orders SET (
    autovacuum_vacuum_scale_factor = 0.1,
    autovacuum_analyze_scale_factor = 0.05
);

-- ============================================================================
-- 接続プーリング推奨設定
-- ============================================================================

-- max_connections: 200
-- shared_buffers: 4GB (総メモリの25%)
-- effective_cache_size: 12GB (総メモリの75%)
-- maintenance_work_mem: 1GB
-- checkpoint_completion_target: 0.9
-- wal_buffers: 16MB
-- default_statistics_target: 100
-- random_page_cost: 1.1 (SSD使用時)
-- effective_io_concurrency: 200 (SSD使用時)
-- work_mem: 20MB
-- max_worker_processes: 8
-- max_parallel_workers_per_gather: 4
-- max_parallel_workers: 8
