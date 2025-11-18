# イベントリスナー設計書 - OrderListener

## 基本情報

| 項目 | 内容 |
|------|------|
| **Event Listener ID** | EL-001 |
| **Javaクラス名** | `OrderListener` |
| **パッケージ** | `com.example.modulith.poc.model.order.eventlistener` |

## イベントリスナー一覧

| # | リスナー名 | 受信イベント | 発行イベント |
|---|-----------|------------|------------|
| 1 | onOrderCreate | `OrderCreate` | `ItemAllocate` |
| 2 | onItemAllocateComplete | `ItemAllocateComplete` | `OrderCreateComplete` |

---

## イベントリスナー詳細

### 1. onOrderCreate

**処理概要:**  
注文作成イベントを受信し、在庫モジュールに対して在庫引当リクエストを発行します。受信した注文情報（商品ID、数量）をそのまま在庫引当イベントに変換して連携します。

**受信イベント:**  
`com.example.modulith.poc.event.order.OrderCreate`

**発行イベント:**  
`com.example.modulith.poc.event.inventory.ItemAllocate`

---

### 2. onItemAllocateComplete

**処理概要:**  
在庫引当完了イベントを受信し、注文確定イベントを発行します。在庫の引当が正常に完了したことを確認し、注文プロセスを完了させます。

**受信イベント:**  
`com.example.modulith.poc.event.inventory.ItemAllocateComplete`

**発行イベント:**  
`com.example.modulith.poc.event.order.OrderCreateComplete`

---

