# 要件ドキュメント更新

## Target
以下のディレクトリに格納されているドキュメントを更新する場合に利用するプロセス:

```
doc/
├── business-rule/
└── user-story/
```

## Prompt Example

```text
[変更内容をPromptとして記載する]
```

- `req-doc-update` agent を利用する

## Phase 1: Change Analysis

### Input
プロダクトオーナーから提示された新規要件もしくは要件変更

### Process
1. 作業ファイルの特定
    - 新規要件の場合: 各ディレクトリに格納されている `XXX_TEMPLATE.md` を用いて新規ファイルを作成する
    - 要件変更の場合: 要件に対応する既存ファイルを特定する
2. [input]の内容をテンプレートに従い反映
3. プロダクトオーナー及びSVレビュー

### Output
- Markdownファイル

### Checklist
- [ ] テンプレートの内容を過不足なく反映している
