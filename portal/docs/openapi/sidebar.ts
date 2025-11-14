import type { SidebarsConfig } from "@docusaurus/plugin-content-docs";

const sidebar: SidebarsConfig = {
  apisidebar: [
    {
      type: "doc",
      id: "openapi/fashion-ec-order-to-shipment-platform-api",
    },
    {
      type: "category",
      label: "Authentication",
      items: [
        {
          type: "doc",
          id: "openapi/register-user",
          label: "ユーザー登録",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/login",
          label: "ログイン",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/logout",
          label: "ログアウト",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/refresh-token",
          label: "トークンリフレッシュ",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/request-password-reset",
          label: "パスワードリセット要求",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/confirm-password-reset",
          label: "パスワードリセット確定",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "Users",
      items: [
        {
          type: "doc",
          id: "openapi/get-my-profile",
          label: "自分のプロフィール取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/update-my-profile",
          label: "プロフィール更新",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/get-my-addresses",
          label: "住所一覧取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-address",
          label: "住所登録",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-address-detail",
          label: "住所詳細取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/update-address",
          label: "住所更新",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/delete-address",
          label: "住所削除",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/get-order-history",
          label: "注文履歴取得",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "Products",
      items: [
        {
          type: "doc",
          id: "openapi/get-products",
          label: "商品一覧取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/get-product-detail",
          label: "商品詳細取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/get-product-skus",
          label: "商品SKU一覧取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/search-products",
          label: "商品検索",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "Cart",
      items: [
        {
          type: "doc",
          id: "openapi/get-cart",
          label: "カート取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/clear-cart",
          label: "カートクリア",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/add-to-cart",
          label: "カートに商品を追加",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/update-cart-item",
          label: "カート商品の数量更新",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/remove-cart-item",
          label: "カートから商品を削除",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/validate-cart",
          label: "カート検証",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "Orders",
      items: [
        {
          type: "doc",
          id: "openapi/get-orders",
          label: "注文一覧取得（管理者用）",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-order",
          label: "注文作成",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-order-detail",
          label: "注文詳細取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/cancel-order",
          label: "注文キャンセル",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-order-status",
          label: "注文ステータス取得",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "Inventory",
      items: [
        {
          type: "doc",
          id: "openapi/get-sku-inventory",
          label: "SKU在庫取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/update-sku-inventory",
          label: "在庫数更新",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/reserve-inventory",
          label: "在庫予約",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/release-inventory",
          label: "在庫予約解放",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-inventory-adjustments",
          label: "在庫調整履歴取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-inventory-adjustment",
          label: "在庫調整",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "Promotions",
      items: [
        {
          type: "doc",
          id: "openapi/get-promotions",
          label: "プロモーション一覧取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/get-promotion-detail",
          label: "プロモーション詳細取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/validate-coupon",
          label: "クーポン検証",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/calculate-discount",
          label: "割引計算",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "Shipments",
      items: [
        {
          type: "doc",
          id: "openapi/get-shipments",
          label: "出荷一覧取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/get-shipment-detail",
          label: "出荷詳細取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/get-shipment-tracking",
          label: "配送追跡情報取得",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "Returns",
      items: [
        {
          type: "doc",
          id: "openapi/get-returns",
          label: "返品一覧取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-return",
          label: "返品・交換申請",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-return-detail",
          label: "返品詳細取得",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/approve-return",
          label: "返品承認",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/reject-return",
          label: "返品却下",
          className: "api-method post",
        },
      ],
    },
  ],
};

export default sidebar.apisidebar;
