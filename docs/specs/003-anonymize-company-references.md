# SPEC-003: 社名関連の完全匿名化

- **ステータス:** レビュー中
- **担当:** 詳細設計・実装・テストは Codex、要件定義・基本設計・最終レビューは Claude Code

## 背景・課題

本リポジトリをPrivateからPublicに切り替えるにあたり実施した精査で、実データ・秘密情報は既に除去済みと確認できたが、実在の元企業名がJavaパッケージ名・DB名・表示文言・ドキュメントに広く残っていることが判明した。ポートフォリオとして公開するにあたり、元企業を特定できないよう匿名化する。

Git履歴の書き換えは行わない(2026-08-21、ユーザー確認済み)。**本タスクは現在のコード・ドキュメントの中身を変更するのみ。**

## 要件

- 実在企業名を特定できる文字列を、現在リポジトリに存在するファイルから全て除去する。
- アプリケーションの動作・見た目（匿名化した表示文言を除く）は変更しない。

## 基本設計

### 命名の置き換えルール(Claude Code決定済み)

| 旧 | 新 |
| :--- | :--- |
| Javaパッケージ `com.[旧組織名].backend` | `com.quotationapp.backend` |
| `build.gradle` の `group` | `[旧group]` → `com.quotationapp` |
| `build.gradle` の `description` | `[旧description]` → `QuotationApp` |
| DB名 `[旧DB名]` | `quotation_db` |
| フロント印刷プレビューの会社名表示（[EditScreen.tsx](../../frontend/src/components/EditScreen.tsx) 内 `[旧社名]`） | `株式会社サンプル` |
| ログイン画面のプレースホルダー（[LoginPage.tsx](../../frontend/src/components/LoginPage.tsx) 内 `[旧メールアドレス]`） | `user@example.com` |
| READMEタイトルの英語副題 | `[旧英語副題]` → `Quotation Management System` |

### 対象ファイル・作業内容

1. **Javaパッケージ移動**: `backend/src/main/java/com/[旧組織名]/backend/` 配下の全ファイル、`backend/src/test/java/com/[旧組織名]/backend/` 配下の全ファイルを `com/quotationapp/backend/` 配下に移動し、全ファイルの `package` 宣言・`import` 文を書き換える(パッケージ名を変えるだけで、クラス名・ロジックは一切変更しない)。
2. **`backend/build.gradle`**: `group` と `description` を上表の通り変更。
3. **`backend/src/main/resources/application.properties`**: JDBC URLのDB名、ログ出力設定のパッケージ名を新名称に変更。
4. **`backend/src/test/resources/application.properties`**: ログ出力設定のパッケージ名を新名称に変更(H2の`testdb`自体は対象外)。
5. **`frontend/src/components/EditScreen.tsx`**: 会社名表示文字列を変更。
6. **`frontend/src/components/LoginPage.tsx`**: プレースホルダー文字列を変更。
7. **`README.md`**: タイトル、`CREATE DATABASE`例、ディレクトリ構成図中のパッケージパスを更新。
8. **`docs/DATABASE.md`**: `Database Name` の値を更新。
9. **`docs/specs/001-user-branch-id.md`・`docs/specs/002-remove-unused-copy-api.md`**: 本文中のファイルパス参照を新パッケージパスに更新(完了済み仕様書の記録を実態に合わせるため)。

## テスト方針

新規の振る舞いを追加するタスクではなく機械的なリネームのため、以下を確認する。

| # | Given | When | Then |
| :-- | :--- | :--- | :--- |
| TC1 | 上記の置き換えが完了した状態 | リポジトリ全体を旧社名の英語・日本語表記で検索する | ヒット0件 |
| TC2 | パッケージ移動・build.gradle変更後の状態 | `cd backend && ./gradlew build` を実行する | ビルド成功、既存テストが全てパスする(新規テスト追加は不要) |
| TC3 | フロントの文字列変更後の状態 | `cd frontend && npm run build` を実行する | ビルド成功 |
| TC4 | アプリを実際に起動した状態 | 印刷プレビュー画面を表示する | 会社名が「株式会社サンプル」と表示される(手動確認) |

## 受け入れ基準

- [x] TC1: 旧社名を含む文字列がリポジトリ内に残っていない
- [x] TC2: `./gradlew build` が成功し、既存テストが全てパスする
- [x] TC3: `npm run build` が成功する
- [x] TC4: 印刷プレビューの会社名表示が更新されている(表示箇所を確認)
- [x] `docs/DATABASE.md`・README・完了済み仕様書のファイルパス参照が新パッケージパスに更新されている

## スコープ外

- Git履歴の書き換え(コミットメッセージ・過去のファイル内容)
- GitHubアカウント名・リポジトリ名の変更
- コミット作者のメールアドレス変更
- アプリケーションのロジック・挙動の変更(表示文言以外)
