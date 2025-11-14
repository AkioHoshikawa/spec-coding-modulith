# データモデルドキュメント

本ディレクトリには、ファッションEC受注〜出荷プラットフォームのデータモデル定義と運用ガイドが含まれています。

## ディレクトリ構成

```
doc/data/
├── README.md                    # このファイル
├── model/                       # データモデル仕様書
│   ├── data-model-overview.md   # データモデル全体概要
│   ├── user-tables.md          # ユーザー・認証ドメインのテーブル仕様
│   ├── inventory-tables.md     # 商品・在庫ドメインのテーブル仕様
│   ├── order-tables.md         # 注文ドメインのテーブル仕様
│   ├── other-tables.md         # その他ドメインのテーブル仕様
│   └── operations-guide.md     # 運用ガイド
└── schema/                      # DDL定義
    ├── user.sql                # ユーザー・認証テーブル DDL
    ├── inventory.sql           # 商品・在庫テーブル DDL
    ├── order.sql               # 注文テーブル DDL
    ├── promotion.sql           # プロモーションテーブル DDL
    ├── shipment.sql            # 出荷・物流テーブル DDL
    ├── return.sql              # 返品・交換テーブル DDL
    ├── payment.sql             # 決済テーブル DDL
    ├── audit.sql               # 監査ログテーブル DDL
    └── index-management.sql    # インデックス管理・最適化 DDL
```

## クイックスタート

### 1. データベース初期化

```bash
# PostgreSQL 14以降を使用
createdb fashion_ec

# スキーマの作成（順序重要）
psql -d fashion_ec -f schema/user.sql
psql -d fashion_ec -f schema/inventory.sql
psql -d fashion_ec -f schema/order.sql
psql -d fashion_ec -f schema/promotion.sql
psql -d fashion_ec -f schema/shipment.sql
psql -d fashion_ec -f schema/return.sql
psql -d fashion_ec -f schema/payment.sql
psql -d fashion_ec -f schema/audit.sql

# インデックス・最適化設定
psql -d fashion_ec -f schema/index-management.sql
```

### 2. パーティション初期化

```sql
-- 初回のみ実行：今後3ヶ月分のパーティションを作成
SELECT create_monthly_partitions('orders', 3);
SELECT create_monthly_partitions('order_status_history', 3);
SELECT create_monthly_partitions('inventory_transactions', 3);
SELECT create_monthly_partitions('user_auth_events', 3);
SELECT create_monthly_partitions('audit_logs', 3);
SELECT create_monthly_partitions('system_events', 3);
SELECT create_monthly_partitions('payments', 3);
SELECT create_monthly_partitions('payment_transactions', 3);
SELECT create_monthly_partitions('shipments', 3);
SELECT create_monthly_partitions('shipment_trackings', 3);
SELECT create_monthly_partitions('returns', 3);
SELECT create_monthly_partitions('coupon_usages', 3);
```

### 3. 拡張機能の有効化

```sql
-- UUID生成
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- スロークエリ監視
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- 全文検索（日本語対応）
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

## データモデル概要

### ドメイン分割

本システムは以下の8つのドメインで構成されています：

#### 1. ユーザー・認証ドメイン
- **テーブル**: users, user_addresses, user_sessions, user_auth_events, etc.
- **責務**: アカウント管理、認証、セッション管理、監査ログ
- **特徴**: MFA対応、OAuth連携準備、GDPR準拠

#### 2. 商品・在庫ドメイン
- **テーブル**: products, skus, inventory_locations, inventory_stocks, inventory_transactions, inventory_locks
- **責務**: 商品マスタ、SKU管理、在庫数管理、在庫ロック
- **特徴**: 楽観ロック、在庫競合制御、履歴管理

#### 3. 注文ドメイン
- **テーブル**: carts, cart_items, orders, order_lines, order_status_history
- **責務**: カート、注文、ステータス管理
- **特徴**: パーティショニング、スナップショット保存

#### 4. プロモーションドメイン
- **テーブル**: promotions, promotion_rules, coupons, coupon_usages
- **責務**: セール、クーポン、割引ルール
- **特徴**: 優先度制御、JSONB活用

#### 5. 出荷・物流ドメイン
- **テーブル**: shipments, shipment_items, shipment_trackings
- **責務**: 出荷管理、配送追跡、3PL連携
- **特徴**: 外部連携ID保持、追跡イベント記録

#### 6. 返品・交換ドメイン
- **テーブル**: returns, return_items, exchanges, exchange_items
- **責務**: 返品申請、検品、返金、交換処理
- **特徴**: ワークフロー管理、画像保存（JSONB）

#### 7. 決済ドメイン
- **テーブル**: payments, payment_transactions
- **責務**: 決済処理、トランザクション管理
- **特徴**: PCI-DSS準拠、トークン化、外部API連携

#### 8. 監査ログドメイン
- **テーブル**: audit_logs, system_events
- **責務**: 操作監査、システムイベント記録、アラート管理
- **特徴**: 変更履歴（JSONB）、パーティショニング

## 主要な設計原則

### 1. データ整合性
- **外部キー制約**: 参照整合性を保証
- **楽観ロック**: 在庫など競合が発生しやすいテーブルで使用
- **トランザクション境界**: 注文確定時は在庫引当までを1トランザクションで実行

### 2. パフォーマンス
- **パーティショニング**: 時系列データは月次パーティション
- **インデックス戦略**: 複合インデックス、部分インデックス、カバリングインデックス
- **キャッシュ活用**: Redis等で在庫数・セッションをキャッシュ

### 3. スケーラビリティ
- **UUID主キー**: 分散環境での一意性担保
- **読み書き分離**: レプリケーションによる読み取り負荷分散
- **水平スケール**: ステートレス設計によるアプリケーション層のスケールアウト

### 4. データ保護
- **個人情報保護**: 暗号化、マスキング、GDPR対応
- **監査ログ**: すべての重要操作を記録（最低1年保持）
- **バックアップ**: WALアーカイブ、PITR、定期スナップショット

### 5. 運用性
- **自動パーティション管理**: 関数による自動作成・削除
- **監視ビュー**: スロークエリ、テーブルサイズ、インデックス使用率
- **アーカイブ戦略**: 古いデータはS3等へ移行

## データサイズ見積もり

### 3年後の想定データ量

| ドメイン | オンライン | アーカイブ | 合計 |
|---------|-----------|-----------|------|
| ユーザー・認証 | 0.5GB | 1GB | 1.5GB |
| 商品・在庫 | 0.4GB | 15GB | 15.4GB |
| 注文 | 4.2GB | 8.4GB | 12.6GB |
| プロモーション | 0.1GB | 0.2GB | 0.3GB |
| 出荷・物流 | 1.5GB | 4GB | 5.5GB |
| 返品・交換 | 0.3GB | 0.5GB | 0.8GB |
| 決済 | 2.6GB | 5.2GB | 7.8GB |
| 監査ログ | 12.6GB | 25GB | 37.6GB |
| **総計** | **約22GB** | **約60GB** | **約82GB** |

## パフォーマンス目標

| API / 処理 | P95 | P99 |
|-----------|-----|-----|
| 注文確定（在庫引当含む） | 1.5秒 | 2秒 |
| 注文履歴取得 | 300ms | 500ms |
| 在庫参照 | 50ms | 100ms |
| 商品検索 | 200ms | 500ms |
| ログイン・認証 | 100ms | 200ms |

## セキュリティ要件

### 個人情報保護
- パスワード: Argon2/PBKDF2でハッシュ化
- トークン: SHA-256でハッシュ化して保存
- 決済情報: PCI-DSS準拠、カード番号は保存せずトークン化
- 個人情報暗号化: AES-256等での暗号化検討

### アクセス制御
- ロールベースアクセス制御（RBAC）
- 行レベルセキュリティ（RLS）の検討
- 最小権限の原則

### 監査
- すべての重要操作を audit_logs に記録
- 認証イベントは user_auth_events に記録
- IP、UserAgent、リクエストIDを必ず記録

## トラブルシューティング

### よくある問題と解決策

#### 1. スロークエリ

```sql
-- 問題のクエリを特定
SELECT * FROM slow_queries LIMIT 10;

-- 実行計画の確認
EXPLAIN ANALYZE <your_query>;

-- インデックス追加を検討
```

#### 2. 在庫競合エラー

```sql
-- 楽観ロックエラーの場合はリトライ
-- アプリケーション層で指数バックオフ実装
```

#### 3. ディスク容量不足

```sql
-- 大きなテーブルを確認
SELECT * FROM table_sizes LIMIT 10;

-- アーカイブ実行
-- operations-guide.md 参照
```

#### 4. 接続枯渇

```sql
-- アクティブ接続数確認
SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'active';

-- 接続プーリング（PgBouncer等）の導入検討
```

## 参考ドキュメント

- [データモデル全体概要](model/data-model-overview.md)
- [ユーザー・認証テーブル仕様](model/user-tables.md)
- [商品・在庫テーブル仕様](model/inventory-tables.md)
- [注文テーブル仕様](model/order-tables.md)
- [その他テーブル仕様](model/other-tables.md)
- [運用ガイド](model/operations-guide.md)

## メンテナンス

### 定期実行タスク

#### 日次
```bash
# バックアップ
pg_dump -Fc fashion_ec > backup_$(date +%Y%m%d).dump

# 統計情報更新
psql -d fashion_ec -c "SELECT update_table_statistics();"
```

#### 月次
```bash
# パーティション作成（3ヶ月先）
psql -d fashion_ec -c "SELECT create_monthly_partitions('orders', 3);"

# アーカイブ実行（operations-guide.md参照）
```

#### 四半期
```bash
# 古いパーティション削除（アーカイブ後）
psql -d fashion_ec -c "SELECT drop_old_partitions('orders', 12);"

# インデックス再構築
psql -d fashion_ec -c "SELECT * FROM reindex_bloated_indexes(0.3);"
```

## サポート

問題が発生した場合は以下を確認してください：

1. [運用ガイド](model/operations-guide.md)のトラブルシューティングセクション
2. PostgreSQLログファイル
3. アプリケーションログ
4. 監視ダッシュボード（Grafana等）

## ライセンス

本ドキュメントは社内利用を目的としています。

---

**最終更新日**: 2024年11月11日  
**バージョン**: 1.0.0  
**作成者**: データベースチーム