# 見積作成システム (Quotation Management System)

Spring Boot (Backend) と React (Frontend) で構築された Web 見積作成アプリケーションです。
見積の作成、PDF プレビュー、履歴管理、および **Gemini API を使用した OCR 自動読取機能** を備えています。

## 機能概要

- **ログイン認証**: ユーザー情報に基づいたアクセス制御
- **見積作成・編集**:
  - 明細行の追加・削除、並び替え
  - 計算ロジック（小計、消費税、粗利率）の自動計算
  - プレビュー画面のリアルタイム表示・ズーム機能
- **OCR 自動読取 (AI 機能)**:
  - PDF や画像の見積書をアップロードし、Google Gemini API を使用して品名・数量・単価を自動抽出
- **見積検索**: 顧客名、案件名、営業所などでの絞り込み
- **見積コピー**: 過去の見積を複製して新規作成（枝番管理）

## 技術スタック

### Backend

- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Database Access**: MyBatis
- **Database**: MySQL 8.0 (Dev), H2 (Test)
- **Build Tool**: Gradle
- **AI Integration**: Google Gemini API (via HTTP Client)

### Frontend

- **Language**: TypeScript
- **Framework**: React 18 (Vite)
- **Styling**: CSS Modules (Custom CSS)

## セットアップ手順

### 前提条件

- JDK 17 以上
- Node.js 18 以上
- MySQL 8.0

### 1. データベース設定

MySQL にデータベースを作成し、`test/resources/schema.sql` の内容を参考にテーブルを作成します。

```sql
CREATE DATABASE quotation_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Backend 起動

`quotationapp/backend/src/main/resources/application.properties` の DB 接続設定と Gemini API キーを確認・変更してください。

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/quotation_db...
spring.datasource.username=root
spring.datasource.password=your_password
gemini.api.key=YOUR_GEMINI_API_KEY
```

起動コマンド:

```bash
cd quotationapp/backend
./gradlew bootRun
```

サーバーは `http://localhost:8080` で起動します。

### 3. Frontend 起動

```bash
cd src
npm install
npm run dev
```

ブラウザで `http://localhost:5173` (Vite のデフォルト) にアクセスします。
※ `vite.config.ts` で `/api` へのプロキシ設定が正しく行われていることを確認してください。

## OCR 機能について

編集画面の「📎 仕入見積添付」ボタンからファイルをアップロードした後、「🤖 自動読取」ボタンをクリックすると、Gemini API を経由して明細行が自動入力されます。

- 対応ファイル: PDF, 画像 (PNG, JPEG 等)
- API キーの設定が必要です (`application.properties`)。

## ディレクトリ構成

```
.
├── mappers/               # MyBatis XMLマッパー
├── quotationapp/backend/       # Spring Boot ソースコード
│   ├── controller/        # APIエンドポイント
│   ├── dto/               # データ転送オブジェクト
│   ├── entity/            # DBエンティティ
│   ├── repository/        # MyBatis Mapperインターフェース
│   └── service/           # ビジネスロジック (OcrService含む)
├── src/                   # React ソースコード
│   ├── components/        # UIコンポーネント (EditScreen, QuotationList)
│   └── types.ts           # TypeScript型定義
└── test/                  # JUnitテストコード
```
