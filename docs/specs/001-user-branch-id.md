# SPEC-001: ユーザーの所属営業所 (branch_id) 対応

- **ステータス:** 完了（Claude Codeによる最終レビュー承認済み、2026-08-20）
- **担当:** 詳細設計・実装・テストは Codex、要件定義・基本設計・最終レビューは Claude Code

## 背景・課題

[docs/REQUIREMENTS.md](../REQUIREMENTS.md) 2章の通り、利用者は各拠点(営業所)に所属するメディカル担当者を想定しているが、`users` テーブルに所属営業所を保持するカラムが存在しない。結果、フロントエンドはログインユーザーの `branchId` を常に `undefined` として受け取り、[LoginPage.tsx](../../frontend/src/components/LoginPage.tsx) の `loggedInUser.branchId || 9443` というフォールバックにより、**誰がログインしても見積編集画面の営業所初期値が固定値(ID: 9443、広島中営業所)になっている**。

## 要件

- ログインユーザーは1つの所属営業所 (branch_id) を持つ。
- ログインAPI (`POST /api/login`) のレスポンスに、そのユーザーの所属営業所IDを含める。
- フロントエンドは、返却された `branchId` を編集画面の営業所初期値として使用する。既存のフォールバック `|| 9443` は、`branch_id` が未設定 (NULL) な異常系向けの安全策としてそのまま残してよい。

## 基本設計

### DBスキーマ

- `users` テーブルに `branch_id INT NULL` を追加。FK制約: `branch_id REFERENCES branches(id)`。NULL許容とする(既存レコード・移行を容易にするため)。
- 対象ファイル: `backend/src/test/resources/schema.sql`(テスト用H2スキーマ)、[docs/DATABASE.md](../DATABASE.md)(設計書。`users` テーブル定義に行追加)。
- 本番/開発MySQLへのマイグレーションSQLの要否はCodexの判断に委ねる。既存 `users` データへの `branch_id` 投入は本タスクのスコープ外(実データはリポジトリに含まれていないため)。

### バックエンド

- `backend/src/main/java/com/saywell/backend/entity/User.java`: `private Integer branchId;` を追加。
- `backend/src/main/resources/mappers/usermapper.xml`: `findByEmail` 等の該当SELECTに `branch_id` カラムを追加。
- `backend/src/main/java/com/saywell/backend/dto/LoginResponse.java`: `branchId` フィールドを追加(コンストラクタ含む)。
- `backend/src/main/java/com/saywell/backend/service/UserService.java`: `login()` 内で `LoginResponse` 生成時に `user.getBranchId()` を渡す。
- 他に `UserDto.java` 等が存在する場合は同様に追加する。

### フロントエンド

- `frontend/src/types.ts` の `User` 型は既に `branchId: number` を持つため型定義の変更は不要。
- `frontend/src/components/LoginPage.tsx` のフォールバック処理はそのまま維持し、「branch_id 未設定時の異常系フォールバック」である旨をコメントで明記する。

## テスト方針

実装は [docs/DEVELOPMENT.md](../DEVELOPMENT.md) のTDDルールに従い、以下のケースをRed→Green→Refactorで実装する。

| # | Given | When | Then | 配置先 |
| :-- | :--- | :--- | :--- | :--- |
| TC1 | `branch_id` が設定されたユーザーが存在する | そのユーザーがメールアドレス・パスワードでログインする | レスポンスの `branchId` に設定された営業所IDが返る | `UserServiceTest` |
| TC2 | `branch_id` が `NULL` のユーザーが存在する | そのユーザーがログインする | 例外にならず、`branchId` が `null` としてレスポンスされる | `UserServiceTest` |
| TC3 | 異なる所属営業所を持つ複数ユーザーが存在する | `UserRepository` で各ユーザーを取得する | 取得結果の `branchId` がそれぞれ正しくマッピングされ、ユーザーごとに異なる(NULLのケースも含む) | `UserRepositoryTest` |

既存のログイン成功/失敗系のテストが壊れていないことも合わせて確認する。

## 受け入れ基準

- [x] `users` テーブルに `branch_id` カラムが追加されている
- [x] TC1: `branch_id` 設定済みユーザーのログインで `LoginResponse.branchId` が正しい値を返す
- [x] TC2: `branch_id` が `NULL` のユーザーでログインしても例外にならず `branchId=null` が返る
- [x] TC3: `UserRepository` が `branch_id`(NULL・非NULL両方)を正しくマッピングする
- [x] TC1〜TC3のテストコードが実装前に失敗し、実装後にパスすることを確認済み(後付けテストでない)

## スコープ外

- 既存ユーザーデータへの `branch_id` 一括投入
- 営業所変更のUI(管理画面等)
