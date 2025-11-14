# APIドキュメント

このディレクトリには、ファッションECの受注〜出荷プラットフォームのOpenAPI 3.0仕様が含まれています。

## ディレクトリ構成

APIドキュメントは保守性を高めるためにモジュール構造で整理されています：

```
doc/api/
├── README.md                 # このファイル
├── openapi.yaml             # OpenAPIメインエントリーポイント
├── components/              # 再利用可能なコンポーネント
│   ├── schemas/            # データモデル
│   ├── parameters/         # 再利用可能なパラメータ
│   ├── responses/          # 再利用可能なレスポンス
│   └── security/           # セキュリティスキーム
└── paths/                  # APIエンドポイント
    ├── auth/              # 認証エンドポイント
    ├── users/             # ユーザー管理エンドポイント
    ├── products/          # 商品エンドポイント
    ├── cart/              # ショッピングカートエンドポイント
    ├── orders/            # 注文エンドポイント
    ├── inventory/         # 在庫管理エンドポイント
    ├── promotions/        # プロモーションエンドポイント
    ├── shipments/         # 出荷エンドポイント
    └── returns/           # 返品・交換エンドポイント
```

## APIモジュール

### 1. 認証・認可 (`/auth`)
- ユーザー登録
- ログイン/ログアウト
- パスワードリセット
- OAuth 2.0 / OIDC連携

### 2. ユーザー管理 (`/users`)
- プロフィール管理
- 住所管理
- 注文履歴
- ユーザー設定

### 3. 商品カタログ (`/products`)
- 商品一覧と検索
- 商品詳細
- SKU情報
- 在庫確認

### 4. ショッピングカート (`/cart`)
- カートの作成と管理
- 商品の追加/更新/削除
- カート検証

### 5. 注文管理 (`/orders`)
- 注文作成と確定
- 注文ステータス追跡
- 注文履歴
- キャンセル処理

### 6. 在庫管理 (`/inventory`)
- 在庫レベル管理
- 在庫予約/ロック
- 在庫引当
- 在庫調整

### 7. プロモーション (`/promotions`)
- プロモーション一覧
- クーポン検証
- 割引計算
- 予約販売管理

### 8. 出荷管理 (`/shipments`)
- 出荷作成
- 配送追跡情報
- 3PL連携
- 配送ステータス更新

### 9. 返品・交換 (`/returns`)
- 返品・交換申請
- 返品ステータス追跡
- 返金処理

## 使用方法

### APIドキュメントの閲覧

1. **Swagger UIを使用:**
   ```bash
   # swagger-uiをインストール（未インストールの場合）
   npm install -g swagger-ui-watcher
   
   # APIドキュメントを起動
   swagger-ui-watcher openapi.yaml
   ```

2. **Redocを使用:**
   ```bash
   npx @redocly/cli preview-docs openapi.yaml
   ```

3. **オンラインエディタ:**
   - `openapi.yaml`を[Swagger Editor](https://editor.swagger.io/)にアップロード
   - または[Redocly](https://redocly.com/)を使用

### バリデーション

```bash
# Redocly CLIをインストール
npm install -g @redocly/cli

# OpenAPI仕様を検証
redocly lint openapi.yaml

# 単一ファイルにバンドル（オプション）
redocly bundle openapi.yaml -o openapi-bundle.yaml
```

## 開発ガイドライン

### 新しいエンドポイントを追加する場合:

1. 適切な`paths/`サブディレクトリにパスファイルを作成
2. 必要に応じて`components/schemas/`にスキーマを定義
3. `components/responses/`から共通レスポンスを参照
4. メインの`openapi.yaml`を更新して新しいパスを含める

### 命名規則:

- **ファイル名:** ケバブケースを使用（例: `order-items.yaml`）
- **スキーマ名:** パスカルケースを使用（例: `OrderItem`）
- **プロパティ名:** キャメルケースを使用（例: `orderId`）
- **パスパラメータ:** キャメルケースを使用（例: `{orderId}`）

### ベストプラクティス:

- スキーマには必ず例を含める
- すべての可能なエラーレスポンスを文書化する
- 再利用性のために`$ref`を使用する
- 個別のファイルは焦点を絞って小さく保つ
- すべてのスキーマとプロパティに説明を追加する
- 一貫したエラーレスポンス形式を定義する

## バージョン履歴

- **v1.0.0** - 初期API仕様（現在）

## お問い合わせ

API仕様に関する質問や提案については、開発チームにお問い合わせください。
