# データモデル運用ガイド

## 目次
1. [パーティション管理戦略](#パーティション管理戦略)
2. [インデックス最適化](#インデックス最適化)
3. [データアーカイブ手順](#データアーカイブ手順)
4. [バックアップ・リストア戦略](#バックアップリストア戦略)
5. [パフォーマンス監視](#パフォーマンス監視)
6. [容量計画](#容量計画)
7. [データ移行手順](#データ移行手順)

---

## パーティション管理戦略

### 対象テーブル
以下のテーブルは月次パーティションで管理します：

- `orders` (ordered_at)
- `order_status_history` (changed_at)
- `inventory_transactions` (created_at)
- `user_auth_events` (created_at)
- `audit_logs` (created_at)
- `system_events` (created_at)
- `payments` (created_at)
- `payment_transactions` (created_at)
- `shipments` (created_at)
- `shipment_trackings` (created_at)
- `returns` (requested_at)
- `coupon_usages` (used_at)

### 自動パーティション作成

```sql
-- 毎月1日に実行（cron設定推奨）
SELECT create_monthly_partitions('orders', 3);
SELECT create_monthly_partitions('audit_logs', 3);
-- その他のテーブルも同様
```

### パーティション削除（アーカイブ後）

```sql
-- 12ヶ月より古いパーティションを削除
-- ※アーカイブ完了後に実行すること
SELECT drop_old_partitions('orders', 12);
SELECT drop_old_partitions('audit_logs', 12);
```

### パーティションプルーニングの確認

```sql
-- クエリプランでパーティションプルーニングが効いているか確認
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM orders 
WHERE ordered_at >= '2024-11-01' AND ordered_at < '2024-12-01';
-- Partition Pruner が動作していることを確認
```

---

## インデックス最適化

### インデックス使用率の監視

```sql
-- 使用されていないインデックスの検出
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0 
  AND pg_relation_size(indexrelid) > 10 * 1024 * 1024 -- 10MB以上
ORDER BY pg_relation_size(indexrelid) DESC;

-- 使用されていないインデックスは削除を検討
```

### インデックス肥大化の検出と再構築

```sql
-- 肥大化したインデックスの再構築
SELECT * FROM reindex_bloated_indexes(0.3);

-- 手動でインデックス再構築（オンライン）
REINDEX INDEX CONCURRENTLY idx_orders_user_id;
```

### 複合インデックスの順序

クエリの WHERE / ORDER BY の順序に合わせてインデックスカラムを配置：

```sql
-- ❌ 非効率
CREATE INDEX idx_orders_bad ON orders(order_status, user_id);
SELECT * FROM orders WHERE user_id = ? ORDER BY ordered_at DESC;

-- ✅ 効率的
CREATE INDEX idx_orders_good ON orders(user_id, ordered_at DESC);
SELECT * FROM orders WHERE user_id = ? ORDER BY ordered_at DESC;
```

---

## データアーカイブ手順

### アーカイブ対象データの抽出

#### 1. 完了注文のアーカイブ（1年経過）

```sql
-- アーカイブ対象の確認
SELECT COUNT(*), 
       pg_size_pretty(SUM(pg_column_size(orders.*))) 
FROM orders 
WHERE ordered_at < CURRENT_DATE - INTERVAL '1 year'
  AND order_status = 'COMPLETED';

-- S3へエクスポート（ParquetまたはCSV）
COPY (
    SELECT * FROM orders 
    WHERE ordered_at < CURRENT_DATE - INTERVAL '1 year'
      AND order_status = 'COMPLETED'
) TO PROGRAM 'aws s3 cp - s3://bucket/archive/orders/2023/orders.parquet'
WITH (FORMAT PARQUET);
```

#### 2. 注文明細のアーカイブ

```sql
-- 親注文と連動してアーカイブ
COPY (
    SELECT ol.* 
    FROM order_lines ol
    JOIN orders o ON ol.order_id = o.order_id
    WHERE o.ordered_at < CURRENT_DATE - INTERVAL '1 year'
      AND o.order_status = 'COMPLETED'
) TO PROGRAM 'aws s3 cp - s3://bucket/archive/order_lines/2023/order_lines.parquet'
WITH (FORMAT PARQUET);
```

#### 3. 監査ログのアーカイブ

```sql
-- 1年以上前の監査ログをアーカイブ
COPY (
    SELECT * FROM audit_logs 
    WHERE created_at < CURRENT_DATE - INTERVAL '1 year'
) TO PROGRAM 'aws s3 cp - s3://bucket/archive/audit_logs/2023/audit_logs.parquet'
WITH (FORMAT PARQUET);
```

### アーカイブ後のデータ削除

```sql
-- アーカイブ確認後、古いパーティションを削除
DROP TABLE IF EXISTS orders_2023_01;
DROP TABLE IF EXISTS orders_2023_02;
-- ... 以降のパーティションも同様
```

### アーカイブデータのクエリ（Athena/Prestなど）

```sql
-- S3上のParquetファイルをAthenaでクエリ
CREATE EXTERNAL TABLE archived_orders (
    order_id STRING,
    order_number STRING,
    user_id STRING,
    -- ...
)
STORED AS PARQUET
LOCATION 's3://bucket/archive/orders/';

-- 過去データの分析
SELECT DATE_TRUNC('month', ordered_at) AS month,
       COUNT(*) AS order_count,
       SUM(total_amount) AS total_sales
FROM archived_orders
WHERE ordered_at BETWEEN '2023-01-01' AND '2023-12-31'
GROUP BY 1
ORDER BY 1;
```

---

## バックアップ・リストア戦略

### 継続的バックアップ（WALアーカイブ）

```bash
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://bucket/wal-archive/%f'
```

### 定期フルバックアップ

```bash
# 毎日深夜2時にフルバックアップ（cron設定）
0 2 * * * pg_basebackup -D /backup/$(date +\%Y\%m\%d) -Ft -z -P

# S3へアップロード
aws s3 sync /backup/ s3://bucket/db-backups/
```

### スナップショットバックアップ（AWS RDS）

```bash
# AWS CLIで日次スナップショット作成
aws rds create-db-snapshot \
  --db-instance-identifier mydb \
  --db-snapshot-identifier mydb-snapshot-$(date +%Y%m%d)
```

### ポイントインタイムリカバリ（PITR）

```bash
# 特定時刻へのリストア
pg_basebackup + WALリプレイで特定時刻まで復元
```

### バックアップ保持期間

| バックアップタイプ | 保持期間 |
|-------------------|---------|
| WALアーカイブ | 30日間 |
| 日次フルバックアップ | 30日間 |
| 週次スナップショット | 3ヶ月 |
| 月次スナップショット | 1年 |

---

## パフォーマンス監視

### 主要メトリクス

#### 1. スロークエリの監視

```sql
-- pg_stat_statements拡張を有効化
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- スロークエリTop 20
SELECT * FROM slow_queries LIMIT 20;
```

#### 2. テーブルサイズの監視

```sql
-- テーブルサイズTop 10
SELECT * FROM table_sizes LIMIT 10;
```

#### 3. 接続数の監視

```sql
SELECT 
    COUNT(*) AS total_connections,
    COUNT(*) FILTER (WHERE state = 'active') AS active_connections,
    COUNT(*) FILTER (WHERE state = 'idle') AS idle_connections
FROM pg_stat_activity;
```

#### 4. キャッシュヒット率

```sql
SELECT 
    sum(heap_blks_read) AS heap_read,
    sum(heap_blks_hit) AS heap_hit,
    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) AS cache_hit_ratio
FROM pg_statio_user_tables;
-- cache_hit_ratio は 0.99 以上が理想
```

### アラート設定

| メトリクス | 閾値 | アクション |
|-----------|------|-----------|
| スロークエリ（P99） | > 2秒 | クエリ最適化 |
| CPU使用率 | > 80% | スケールアップ |
| 接続数 | > 80% of max_connections | プーリング設定見直し |
| ディスク使用率 | > 80% | アーカイブ実行 |
| レプリケーション遅延 | > 10秒 | ネットワーク/負荷調査 |

---

## 容量計画

### 年次データ増加予測

| 年度 | 総データ量（オンライン） | 増加量 |
|-----|----------------------|--------|
| Year 1 | 10GB | +10GB |
| Year 2 | 18GB | +8GB |
| Year 3 | 25GB | +7GB |

### ストレージ拡張タイミング

- ディスク使用率が70%を超えたら拡張を計画
- 80%で即座に拡張実行
- 余裕を持って20%以上の空き容量を確保

### コスト最適化

- ホットデータ: SSD（高速・高コスト）
- ウォームデータ: HDD（中速・中コスト）
- コールドデータ: S3 Standard-IA（低速・低コスト）
- アーカイブ: S3 Glacier（アクセス稀・最低コスト）

---

## データ移行手順

### 初期データロード

```bash
# CSVからの一括ロード
COPY users(user_id, email, password_hash, first_name, last_name, created_at)
FROM '/data/users.csv'
WITH (FORMAT CSV, HEADER true);
```

### ゼロダウンタイム移行（論理レプリケーション）

```sql
-- 発行側（旧DB）
CREATE PUBLICATION migration_pub FOR ALL TABLES;

-- 購読側（新DB）
CREATE SUBSCRIPTION migration_sub 
CONNECTION 'host=old-db port=5432 dbname=mydb' 
PUBLICATION migration_pub;

-- レプリケーション遅延の確認
SELECT * FROM pg_stat_subscription;
```

### データ整合性検証

```sql
-- レコード数の比較
SELECT 'users' AS table_name, COUNT(*) FROM users
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
-- ...

-- チェックサムの比較（サンプリング）
SELECT MD5(string_agg(order_id::TEXT, ',' ORDER BY order_id))
FROM (SELECT order_id FROM orders LIMIT 10000) t;
```

---

## 定期メンテナンスチェックリスト

### 日次
- [ ] スロークエリログ確認
- [ ] ディスク使用率確認
- [ ] レプリケーション遅延確認

### 週次
- [ ] テーブルサイズ増加率確認
- [ ] インデックス使用率確認
- [ ] バックアップ成功確認

### 月次
- [ ] パーティション作成（3ヶ月先まで）
- [ ] 統計情報更新（ANALYZE）
- [ ] 古いパーティション削除（アーカイブ後）
- [ ] インデックス肥大化確認・再構築

### 四半期
- [ ] データアーカイブ実行
- [ ] 容量計画レビュー
- [ ] パフォーマンスベンチマーク実施
- [ ] DR（災害復旧）テスト実施
