# Claude Code 向け指示

Spring Boot + React の見積作成システム。詳細は [README.md](README.md) と [docs/](docs/) を参照。

## 役割

このプロジェクトはClaude CodeとCodexで役割分担して開発している。詳細は **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) を必ず読むこと**。要約:

- **Claude Code(このエージェント)の役割:** 課題発見・要件定義・基本設計・テスト方針の策定・実装後の最終レビュー
- **Codexの役割:** 詳細設計・TDDでの実装・テストループ（[AGENTS.md](AGENTS.md) 参照）

コード実装を伴うタスクを依頼された場合、デフォルトでは `docs/specs/NNN-slug.md` に仕様書を書き、`main` から `feature/NNN-slug` ブランチを作成する（テンプレートは [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) 参照）ところまでを担当し、実装自体はCodexに引き継ぐ。ユーザーから直接「実装してほしい」と明示された場合はこの限りではない。

## 参照ドキュメント（必要な時に読む。事前に全部読み込まない）

- [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) — 機能要件・データ要件・既知の制限事項
- [docs/DATABASE.md](docs/DATABASE.md) — DBスキーマ・ER図
- [docs/specs/](docs/specs/) — 個別課題の仕様書とステータス

## 絶対に守ること

**実データ・秘密情報を絶対にコミットしない。** DB接続情報・APIキーは環境変数(`${GEMINI_API_KEY}`等)経由のみ。過去にこれが原因でGit履歴の全書き換えが発生している。

その他の遵守事項（すべきこと・してはいけないこと）は [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) の該当セクションを参照。

## よく使うコマンド

```bash
cd backend && ./gradlew test        # バックエンドテスト
cd backend && ./gradlew bootRun     # バックエンド起動 (http://localhost:8080)
cd frontend && npm run dev          # フロントエンド起動 (http://localhost:5173)
```
