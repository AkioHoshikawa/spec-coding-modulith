# 実行アーキテクチャ (Backend)

## 1. 概要

本システムは、ファッションECにおける受注から出荷までのプラットフォームを提供するバックエンドアプリケーションである。実行アーキテクチャとして **モジュラーモノリス（Modular Monolith）** パターンを採用し、Spring Modulithフレームワークを活用することで、ドメイン境界を明確にしながらも単一デプロイメントユニットとして運用する。

### 主要な設計方針
- **モジュール境界の厳格化**: ドメインモジュール間の依存関係を制限し、疎結合な設計を実現
- **イベント駆動アーキテクチャ**: モジュール間の通信を非同期イベントで実現し、柔軟性と拡張性を確保
- **トランザクション境界の明確化**: ドメインごとにトランザクション範囲を定義し、整合性を担保
- **将来のマイクロサービス化への対応**: モジュール境界を維持することで、必要に応じてマイクロサービスへの分割を可能にする

## 2. アーキテクチャパターン

### 2.1 モジュラーモノリス (Modular Monolith)

#### アーキテクチャ構造
本システムは以下のモジュール構造で構成される：

```
com.example.modulith.poc
├── core                    # 共通インフラストラクチャ
│   ├── event              # イベント基盤（EventBase, EventHeader, EventMapper）
│   └── controller         # イベント調整コントローラ
├── event                   # イベント契約定義
│   ├── order              # 注文関連イベント
│   └── inventory          # 在庫関連イベント
├── channel                 # 外部チャネル層
│   └── web                # Web API（REST）
├── model                   # ドメインモデル層
│   ├── order              # 注文ドメイン
│   └── inventory          # 在庫ドメイン
└── config                  # アプリケーション設定
```

#### モジュール間の依存関係ルール
1. **レイヤー階層**:
   - `channel` → `model` (ドメインサービスを呼び出し可能)
   - `model` → `core` (共通機能を利用可能)
   - `model` → `event` (イベント発行・購読可能)
   - `model` ↔ `model` (直接依存禁止、イベント経由のみ)

2. **モジュール境界の強制**:
   - Spring Modulithの`detection-strategy: explicitly-annotated`により、`package-info.java`で明示的にマークされたモジュールのみを認識
   - モジュール境界を跨ぐ直接的なメソッド呼び出しは禁止（コンパイル時にチェック）
   - モジュール間の通信はイベントまたは公開APIインターフェースを通じてのみ実施

#### モジュラーモノリスの利点
- **開発速度**: 単一リポジトリ・単一デプロイメントによる効率的な開発
- **トランザクション**: 必要に応じて複数ドメインをまたぐトランザクション処理が可能
- **運用コスト**: マイクロサービスと比較してインフラコストと運用複雑性が低い
- **進化可能性**: ドメイン境界が明確なため、将来的なマイクロサービス化が容易

### 2.2 イベント駆動アーキテクチャ (Event-Driven Architecture)

#### イベント通信パターン
モジュール間の通信は、Spring Modulithの **ApplicationEventPublisher** を活用したイベント駆動方式で実現する。

##### イベントフロー例: 注文確定処理
```
1. Web API (channel.web)
   ↓ publishEvent(OrderCreate)
2. Order Domain (model.order)
   - OrderListener が OrderCreate を受信
   - 注文エンティティを生成
   - publishEvent(OrderCreateComplete)
   ↓
3. Inventory Domain (model.inventory)
   - InventoryListener が OrderCreateComplete を受信
   - 在庫引当処理を実行
   - publishEvent(ItemAllocate)
   ↓ (在庫引当完了)
   - publishEvent(ItemAllocateComplete)
```

#### イベントの種類と責務
1. **コマンドイベント** (例: `OrderCreate`, `ItemAllocate`)
   - 処理の開始を指示するイベント
   - 単一のリスナーが処理を担当

2. **完了イベント** (例: `OrderCreateComplete`, `ItemAllocateComplete`)
   - 処理の完了を通知するイベント
   - 複数のリスナーが購読可能（次工程のトリガー）

#### イベント基盤の機能
- **EventBase**: すべてのイベントが継承する基底クラス
  - `EventHeader`: イベントメタデータ（eventId, timestamp, correlationId等）
  - ペイロード: ドメイン固有のデータ

- **Event Publication Registry**: Spring Modulithの機能により、イベント発行履歴を永続化
  - 未完了イベントの検出と再発行
  - アプリケーション再起動時の自動リトライ（`republish-outstanding-events-on-restart: true`）
  - 30日間の保持期間（`retention-policy: P30D`）

- **Event Externalization**: 将来的な外部システム連携に向けた準備
  - イベントを外部メッセージブローカー（Kafka, RabbitMQ等）に転送可能
  - 現在は有効化されているが、外部ブローカー未接続（設定: `externalization.enabled: true`）

### 2.3 レイヤードアーキテクチャ

各ドメインモジュール内部では、以下のレイヤー構造を採用：

```
model.order (例)
├── entity              # エンティティ（永続化対象）
├── service             # ドメインサービス（ビジネスロジック）
├── repository          # データアクセス層（Spring Data JPA）
├── eventlistener       # イベントリスナー
└── package-info.java   # モジュール定義
```

#### 各レイヤーの責務
- **Entity**: ドメインオブジェクトの状態とライフサイクル管理
- **Service**: トランザクション境界とビジネスロジックの実装
- **Repository**: データベースアクセスの抽象化（Spring Data JPA）
- **EventListener**: モジュール間イベントの受信と処理委譲

## 3. 通信パターン

### 3.1 同期通信 (Synchronous Communication)

#### REST API (HTTP/JSON)
- **プロトコル**: HTTP/1.1, HTTPS (TLS 1.2+)
- **フォーマット**: JSON (Content-Type: application/json)
- **フレームワーク**: Spring WebFlux（リアクティブスタック）
- **エンドポイント例**:
  - `POST /api/orders` - 注文作成
  - `GET /api/orders/{orderId}` - 注文取得
  - `PUT /api/orders/{orderId}/cancel` - 注文キャンセル

#### 同期通信の使用場面
- クライアント（フロントエンド、外部システム）からのリクエスト受信
- 即座にレスポンスを返す必要がある操作（問い合わせ、単純なCRUD）
- トランザクション境界内での処理完了を保証する場合

### 3.2 非同期通信 (Asynchronous Communication)

#### モジュール間イベント通信
- **メカニズム**: Spring ApplicationEventPublisher + Spring Modulith Event Publication
- **実行モデル**: 
  - デフォルトは同一スレッドで同期実行
  - `@Async`アノテーションによる非同期実行も可能（`@EnableAsync`有効化済み）
- **トランザクション管理**: 
  - イベントリスナーは新規トランザクションで実行（`@Transactional(propagation = REQUIRES_NEW)`）
  - イベント発行元のトランザクションがコミットされた後にリスナーが実行される保証

#### 非同期通信の使用場面
- ドメイン境界を跨ぐ処理（注文確定後の在庫引当）

## 4. データアーキテクチャ

### 4.1 データベース戦略

#### データベース構成
- **RDBMS**: H2 Database（開発環境）/ PostgreSQL（本番想定）
- **ORM**: Spring Data JPA + Hibernate
- **スキーマ戦略**: Database per Module（論理的分離）
  - 各ドメインモジュールは専用のテーブルプレフィックスを持つ
  - 例: `order_`プレフィックス（order_entity, order_line等）
  - 例: `inventory_`プレフィックス（inventory_location, inventory_transaction等）

#### トランザクション管理
- **トランザクション境界**: Serviceレイヤーで定義（`@Transactional`）
- **分離レベル**: READ_COMMITTED（デフォルト）
- **楽観ロック**: エンティティに`@Version`フィールドを付与し、同時更新を検出
- **悲観ロック**: 在庫引当など競合が多い処理では`LockModeType.PESSIMISTIC_WRITE`を使用

#### データ整合性戦略
1. **強整合性**: 
   - 在庫引当と注文確定は同一トランザクション内で処理
   - 決済失敗時は全体をロールバック

2. **結果整合性**: 
   - 出荷通知、メール送信等はイベント駆動で非同期処理
   - Event Publication Registryによる確実な配信保証

### 4.2 イベントストア

#### Spring Modulith Event Publication Table
- **テーブル**: `event_publication`
- **用途**: 発行されたイベントの永続化と配信保証
- **カラム構成**:
  - `id`: イベントID（UUID）
  - `event_type`: イベントクラス名
  - `serialized_event`: イベントペイロード（JSON）
  - `publication_date`: 発行日時
  - `completion_date`: 処理完了日時（NULL = 未完了）
  - `listener_id`: リスナー識別子

#### イベントリプレイ機能
- アプリケーション再起動時、未完了イベントを自動検出して再発行
- リスナーの冪等性実装により、重複実行を防止
- 30日以上経過したイベントは自動削除（`retention-policy: P30D`）

## 5. レジリエンスパターン

### 5.1 障害対策

#### タイムアウト管理
- **API応答**: P99 2秒以内（SLO）
- **在庫引当処理**: 最大2秒（BR-001に準拠）
- **外部API呼び出し**: 接続3秒、読み取り10秒
- **データベースクエリ**: 1秒でスロークエリ警告

#### リトライポリシー
- **一時的エラー**: 指数バックオフ（初回100ms、最大3回）
  - ネットワークエラー、一時的なDB接続エラー
- **ビジネスエラー**: リトライ不可
  - 在庫不足、バリデーションエラー

### 5.2 データ整合性保証

#### イベント配信保証
- **At-Least-Once配信**: Event Publication Registryによる保証
- **冪等性実装**: リスナー側でイベントIDを記録し、重複実行を防止

#### 在庫整合性制御
- **楽観ロック**: 通常の在庫更新（バージョン番号チェック）
- **悲観ロック**: ピーク時の在庫引当（FOR UPDATE）
- **負数防止**: CHECK制約またはアプリケーションレベルでの検証（BR-001 在庫管理）
- **定期整合性チェック**: 日次バッチで物理在庫と論理在庫の突合

### 5.3 パフォーマンス最適化

#### データベース最適化
- **インデックス戦略**: 
  - 注文検索: `(user_id, order_date)`複合インデックス
  - 在庫引当: `(sku_id, location_id)`複合インデックス
- **N+1問題対策**: `@EntityGraph`によるFetch Join
- **バルク処理**: Batch Insert/Updateの活用

#### 非同期処理
- **@Async実行**: I/O待ちが多い処理（外部API呼び出し、メール送信）


## 6. 参考資料

- [Spring Modulith公式ドキュメント](https://docs.spring.io/spring-modulith/reference/)
- [Spring Boot公式ドキュメント](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- 内部ドキュメント: 
  - `doc/overview/project-overview.md`: プロジェクト概要
  - `doc/business-rule/`: ビジネスルール定義
  - `backend/build/spring-modulith-docs/`: 生成されたモジュール構成図

---

**最終更新日**: 2025-11-18  
**対象バージョン**: Backend v0.0.1-SNAPSHOT
