# SPEC-002: 見積コピー用バックエンドAPI (未使用) の削除

- **ステータス:** 未着手
- **担当:** 詳細設計・実装・テストは Codex、要件定義・基本設計・最終レビューは Claude Code

## 背景・課題

`POST /api/quotations/{id}/copy` エンドポイント (`QuotationController.copy()`) とそれが呼ぶ `QuotationService.copy()`、リクエストDTOの `QuotationCopyRequest` が実装されているが、フロントエンドはこのAPIを一切呼んでいない。

実際の「コピーして新規作成」機能は、フロントエンド側 ([App.tsx](../../frontend/src/App.tsx) の `handleCopyCreate`/`handleTransitionToNew`) で編集画面の状態をリセットし、ユーザーが内容を確認・編集した上で通常の新規作成API (`POST /api/quotations`) を呼ぶ、という2段階のUXで実現されている。

このバックエンドAPIは死んだコードであり、コードベースの見通しを悪くしている。フロント側の2段階UX(コピー内容を確認・修正してから保存できる)の方がユーザーにとって有用なため、実装に合わせてバックエンドAPIを廃止する方針とする(2026-08-20、ユーザー確認済み)。

## 要件

- 使われていない見積コピーAPI関連のコードを削除し、コードベースをシンプルに保つ。
- 既存のフロントエンドのコピー機能 (`handleCopyCreate` 等) の動作は変更しない。

## 基本設計

### 削除対象

- `backend/src/main/java/com/quotationapp/backend/controller/QuotationController.java` の `copy()` メソッド (`POST /{id}/copy`)
- `backend/src/main/java/com/quotationapp/backend/service/QuotationService.java` の `copy()` メソッド
- `backend/src/main/java/com/quotationapp/backend/dto/QuotationCopyRequest.java`
- `backend/src/test/java/com/quotationapp/backend/controller/QuotationControllerTest.java` 内の対応するテストケース(copy関連)
- 上記メソッド/DTOを参照している箇所が他にないか、削除前にリポジトリ全体をgrepして確認すること

### ドキュメント更新

- [docs/REQUIREMENTS.md](../REQUIREMENTS.md) の「7. 既知の制限事項・技術的負債」からこの項目を削除し、コピー機能の実装方式(フロントエンド主導であること)を確定仕様として明記する。

## テスト方針

本タスクは既存機能の削除が目的であり新規のテストケース追加は無いため、[docs/DEVELOPMENT.md](../DEVELOPMENT.md) のTDDサイクルにおける「Red」は「削除前は存在していた `QuotationCopyRequest` を使うテストが削除後にはビルド対象に存在しないこと」、「Green」は削除後も残る全テストのパスとみなす。

| # | Given | When | Then |
| :-- | :--- | :--- | :--- |
| TC1 | `copy()`・`QuotationCopyRequest` を削除した状態 | リポジトリ全体を対象にこれらへの参照を検索する(`grep`等) | 参照が一切残っていない(テストコードも含む) |
| TC2 | コピーAPI関連コード削除後の状態 | `./gradlew test` を実行する | 既存の全テストがパスする(copy関連テストケースは削除済み) |
| TC3 | 削除後のフロントエンド(変更なし) | 既存の見積を開き「コピーして新規作成」→内容編集→「新規保存」を行う | 通常の新規作成として正しく保存される(挙動に変化がない、手動確認) |

## 受け入れ基準

- [ ] TC1: `/api/quotations/{id}/copy` エンドポイントおよび `QuotationCopyRequest` への参照がリポジトリ内に残っていない
- [ ] TC2: `./gradlew test` が全てパスする
- [ ] TC3: フロントエンドの「コピーして新規作成」→新規保存フローが従来通り動作する
- [ ] `docs/REQUIREMENTS.md` が更新されている

## スコープ外

- フロントエンドの「コピーして新規作成」機能自体の挙動変更
