# Codex(コーディングエージェント)向け指示

Spring Boot + React の見積作成システム。詳細は [README.md](README.md) と [docs/](docs/) を参照。

## 役割

このプロジェクトはClaude CodeとCodexで役割分担して開発している。詳細は **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) を必ず読むこと**。要約:

- **Codex(このエージェント)の役割:** `docs/specs/` の仕様書に基づく詳細設計・TDDでの実装・テストループ
- **Claude Codeの役割:** 課題発見・要件定義・基本設計・テスト方針の策定・実装後の最終レビュー（[CLAUDE.md](CLAUDE.md) 参照）

## 作業の進め方

1. 依頼された課題に対応する `docs/specs/NNN-*.md` を読む(存在しない場合は着手せず、Claude Codeに仕様書作成を依頼するようユーザーに伝える)。
2. [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) のTDDルールに従い、仕様書の「テスト方針」(Given-When-Then)を先にテストコードとして書き(Red)、最小実装で通し(Green)、リファクタリングする(Refactor)。これを受け入れ基準の項目ごとに繰り返す。
3. 実装コードとテストコードは同一コミットに含める。
4. 仕様書の「スコープ外」に書かれていることや、依頼されていない周辺リファクタリングはしない。仕様に疑問があれば実装を進める前にユーザー(Claude Codeでのレビュー)に確認する。
5. 完了したら仕様書のステータスを更新する(未着手→実装中→レビュー中)。

## 参照ドキュメント（必要な時に読む。事前に全部読み込まない）

- [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) — 機能要件・データ要件・既知の制限事項
- [docs/DATABASE.md](docs/DATABASE.md) — DBスキーマ・ER図

## 絶対に守ること

**実データ・秘密情報を絶対にコミットしない。** DB接続情報・APIキーは環境変数(`${GEMINI_API_KEY}`等)経由のみ。過去にこれが原因でGit履歴の全書き換えが発生している。

その他の遵守事項（すべきこと・してはいけないこと）は [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) の該当セクションを参照。

## よく使うコマンド

```bash
cd backend && ./gradlew test        # バックエンドテスト
cd backend && ./gradlew bootRun     # バックエンド起動 (http://localhost:8080)
cd frontend && npm run dev          # フロントエンド起動 (http://localhost:5173)
```
