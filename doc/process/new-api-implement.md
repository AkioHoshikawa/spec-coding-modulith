# 新規API実装

## Target
backendの新規APIを実装する。

## Prompt Example
```text
#file:orders.yaml にPOSTメソッドとして定義されている注文作成APIを作成してください。関連するユーザーストーリーは #file:US-001_注文確定時の在庫ロック.md です。
```

- `new-api-implement` agent を利用する
- 作成対象となるAPIのOpenAPI定義があるファイルを指定する
    - 1つのファイルにメソッド違いで複数のAPIが定義されている場合はメソッドを明記する
- OpenAPI定義ファイルの中に複数のAPIが定義されている場合、どのAPIを作成するのかを名称で指定する
- 関連するユーザストーリーの定義ファイルを指定する

## Phase 1: Spec Analysis

### Input
- OpenAPI定義
- ユーザストーリー

### Process
1. 作成もしくは編集するソースコード（ファイル）の特定
2. 作成もしくは修正内容をTaskとして整理
3. 整理したTaskを `.ai-memory/new-api-implement/YYYYMMDD.md` ファイルに保存
    - 同名のファイルが存在する場合は`YYYYMMDD-N.md`のように連番を付与して別ファイルに保存する
4. ファイルの内容をレビューする

### Output
- Markdownファイル

### Checklist
- [ ] 参照しているOpenAPI定義とユーザストーリーが関連していること
    - 関連していない場合は指示ミスのため、作業を中断してInputの確認を行う
- [ ] Business Ruleに沿ったビジネスロジックを作成している
- [ ] データモデルは `doc/data` フォルダに配置されている設計書と平仄が取れている
- [ ] データモデルに合わせてInput Validationが実装されるTaskになっている
- [ ] `doc/architecture/backend-dev-guide.mdに記載されているルールに準拠したTaskになっている
- [ ] テンプレートの内容を過不足なく反映している

## Phase 2: Implement

### Input
- Phase 1で作成したTask

### Process
1. Taskの内容に従い、ソースコードを新規作成もしくは編集する
2. 作成したソースコードが `doc/architecture/backend-dev-guide.md` に記載されているコーディング規約に沿っていることを確認する
3. 作成したソースコードに対してUTスクリプトを作成する
    - ロジックを持つServiceクラスに対してのみ実装する
4. ビルドを行い、コンパイルが通ることと静的解析で警告が発生しないことを確認する
4. 変更をCommitし、Pull Requestを作成する

### Output
- ソースコード

### Checklist
- [ ] 実装とTaskの記載内容の平仄が合っていること
- [ ] `doc/architecture/backend-dev-guide.md`に記載してあるルールに沿った実装になっている
- [ ] ビルドを実行し、エラーもしくは警告が発生していないこと
- [ ] UTテストは境界値を意識したデータパターンで網羅的に実行されるようになっている
- [ ] UTテストは到達不可能なコードを除いて命令網羅率が100%となっている
- [ ] UTテストでMockを用いる場合、実装に関わらず必ずテストが成功するようなMockを作成していない
