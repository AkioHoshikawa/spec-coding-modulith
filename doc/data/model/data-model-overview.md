# データモデル概要

## 目的
本ドキュメントは、ファッションEC受注〜出荷プラットフォームのデータモデル全体像を示す。

## エンティティ関係図（ER図概要）

### コアドメイン
1. **ユーザー・認証ドメイン**
   - users: ユーザーアカウント
   - user_addresses: 配送先住所
   - user_sessions: セッション管理
   - user_auth_events: 認証イベント監査ログ

2. **商品・在庫ドメイン**
   - products: 商品マスタ
   - skus: SKU（在庫管理単位）
   - inventory_locations: 在庫拠点
   - inventory_stocks: 在庫数
   - inventory_transactions: 在庫トランザクション履歴
   - inventory_locks: 在庫ロック

3. **注文ドメイン**
   - carts: カート
   - cart_items: カート明細
   - orders: 注文
   - order_lines: 注文明細
   - order_status_history: 注文ステータス履歴

4. **プロモーションドメイン**
   - promotions: プロモーション
   - promotion_rules: プロモーションルール
   - coupons: クーポン
   - coupon_usages: クーポン使用履歴

5. **出荷・物流ドメイン**
   - shipments: 出荷
   - shipment_items: 出荷明細
   - shipment_trackings: 配送追跡

6. **返品・交換ドメイン**
   - returns: 返品
   - return_items: 返品明細
   - exchanges: 交換

7. **決済ドメイン**
   - payments: 支払い
   - payment_transactions: 決済トランザクション

8. **監査ログドメイン**
   - audit_logs: 操作監査ログ
   - system_events: システムイベントログ

## データ保持ポリシー

### ホットデータ（オンライン）
- **期間**: 最新1年間
- **対象**: orders, order_lines, shipments, inventory_transactions（直近）
- **アクセス頻度**: 高頻度

### ウォームデータ（準オンライン）
- **期間**: 1〜3年
- **対象**: 完了した注文、返品履歴
- **アクセス頻度**: 中頻度（月次レポート、顧客問い合わせ対応）

### コールドデータ（アーカイブ）
- **期間**: 3年以上
- **対象**: 完了した古い注文、監査ログ
- **保存先**: オブジェクトストレージ（S3等）
- **アクセス頻度**: 低頻度（法的要求、監査対応のみ）

## データサイズ見積もり

### 前提条件
- 月間注文数: 100,000件
- 平均注文明細: 2.5件/注文
- 年間ユーザー増: 50,000人
- SKU数: 50,000（アパレル商品の特性上、カラー・サイズ展開で増加）
- 在庫拠点数: 10拠点

### 年間データ増加量
- orders: 1.2M件/年 × 1KB ≒ 1.2GB/年
- order_lines: 3M件/年 × 0.5KB ≒ 1.5GB/年
- inventory_transactions: 10M件/年 × 0.5KB ≒ 5GB/年
- audit_logs: 50M件/年 × 1KB ≒ 50GB/年

### 3年後の想定データ量
- 総データ量: 約200GB（インデックス含む）
- パーティショニング戦略: 日付ベースのパーティション（orders, audit_logs等）

## インデックス戦略
- 主キー: すべてのテーブルでUUID使用（分散環境での一意性担保）
- 複合インデックス: 検索頻度の高いクエリに対して最適化
- パーティションキー: created_at, order_date等の日付カラム

## データ整合性
- トランザクション境界: 注文確定時は在庫ロックとの強整合性を担保
- 楽観ロック: inventory_stocks.version カラムで在庫競合を検出
- 外部キー制約: 参照整合性を保証（一部非正規化を許容）
