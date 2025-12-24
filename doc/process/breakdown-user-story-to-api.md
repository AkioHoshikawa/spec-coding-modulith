# User StoryからAPI設計に分解する

## Target

User Storyの実装に必要なAPIを特定する。調査の結果、既存のAPI設計とGapが存在していた場合はそれを解消するようにOpenAPI定義を更新する。

## Prompt Example

```text
#file:US-001_注文確定時の在庫ロック.md の内容を読み込み、このユーザストーリーを実現するために必要なAPIを列挙してください。
```

- `breakdown-user-story-to-api` agent を利用する
- 実装したいユーザストーリーの定義ファイルを指定する

## Phase 1: User Story Analysis

### Input

- ユーザストーリー
- OpenAPI定義

### Process

1. 指定されたユーザーストーリーで必要となるAPIを推論
2. 推論で導いたAPIと近似するAPIが既存のOpenAPI定義の中にあるかを確認
3. 近似するAPIを発見した場合、そのAPI設計のままでユースケースを実現できるかを確認
4. 必要なAPIを一覧化し、 `.ai-memory/breakdown-user-story-to-api/YYYYMMDD.md` ファイルに保存
    - 同名のファイルが存在する場合は`YYYYMMDD-N.md`のように連番を付与して別ファイルに保存する
    - 既存のAPI設計のままで実現できない場合APIが存在した場合はそのGapの内容と修正案を記載する。    
5. ファイルの内容をレビューする

### Output

- Markdownファイル

### Checklist

- [ ] Business Ruleに沿ったビジネスロジックを作成している
- [ ] データモデルは `doc/data` フォルダに配置されている設計書と平仄が取れている
- [ ] データモデルに合わせてInput Validationが実装されるTaskになっている
- [ ] `doc/architecture/backend-dev-guide.md`に記載されているルールに準拠した設計修正案になっている

## Phase 2: Update API doc

### Input

- Phase 1で作成したmarkdownファイル

### Process

1. インプットに記載されているAPI設計のGap修正案を確認する
2. 確認した修正案を元にOpenAPI定義を修正する
3. 修正されたOpenAPI定義のスキーマが正しいことを `redocly lint` コマンドを用いて検証する

### Output

- 修正されたOpenAPI定義

### Checklist

- [ ] Business Ruleに沿ったビジネスロジックを作成している
- [ ] データモデルは `doc/data` フォルダに配置されている設計書と平仄が取れている
- [ ] データモデルに合わせてInput Validationが実装できるOpenAPI定義となっている
- [ ] `doc/architecture/backend-dev-guide.md`に記載されているルールに準拠した設計修正案になっている
- [ ] OpenAPI定義以外の設計書の修正を行なっていない