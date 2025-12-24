# ECサイト注文管理 APIサーバ

## 概要

Spring Boot及びSpring Modulithで実装された注文管理用のAPIを提供するサーバ。

詳細については `doc` ディレクトリ内の各種設計書を参照。

### 動作環境

- Java 17以上

### 実行方法

```shell
./gradlew bootRun
```

### ビルド方法

```shell
./gradlew bootJar
```

## 実装済みAPI

| Name | Method | Path | Remarks |
| ---- | ------ | ---- | ------- |
| 注文作成 | POST | /v1/orders | 認証/冪等性などTODOあり |

