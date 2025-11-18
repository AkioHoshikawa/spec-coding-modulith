# イベントリスナー設計書テンプレート

## 基本情報

| 項目 | 内容 |
|------|------|
| **Event Listener ID** | EL-XXX |
| **Javaクラス名** | `XxxListener` |
| **パッケージ** | `com.example.modulith.poc.model.[module].eventlistener` |

## イベントリスナー一覧

| # | リスナー名 | 受信イベント | 発行イベント |
|---|-----------|------------|------------|
| 1 | onXxxEvent | `XxxEvent` | `YyyEvent` |
| 2 | onYyyEvent | `YyyEvent` | `ZzzEvent` |

---

## イベントリスナー詳細

### 1. onXxxEvent

**処理概要:**  
処理内容を2-3文で記述。何を受け取り、どのような処理を行い、何を発行するのかを明確に記述します。

**受信イベント:**  
`com.example.modulith.poc.event.[module].XxxEvent`

**発行イベント:**  
`com.example.modulith.poc.event.[module].YyyEvent`

---

### 2. onYyyEvent

**処理概要:**  
処理内容を2-3文で記述。

**受信イベント:**  
`com.example.modulith.poc.event.[module].YyyEvent`

**発行イベント:**  
`com.example.modulith.poc.event.[module].ZzzEvent`

---
