# berry-books-frontend プロジェクト

## 📖 概要

Berry Books オンライン書店の管理者画面（React + TypeScript）です。
顧客一覧を表示し、注文件数と購入冊数の統計情報を確認できます。

## 🚀 セットアップとコマンド実行ガイド

### 前提条件

- Node.js 18以上
- npm または yarn
- berry-books-rest API（バックエンド）が起動していること

> **Note:** ① と ② の手順は、ルートの`README.md`を参照してください。

### ③ 依存関係の確認

このプロジェクトを開始する前に、以下が起動していることを確認してください：

- **① HSQLDBサーバー** （`./gradlew startHsqldb`）
- **② Payara Server** （`./gradlew startPayara`）
- **berry-books プロジェクトでデータベース初期化済み** （`./gradlew :projects:java:berry-books:setupHsqldb`）
- **berry-books-rest がデプロイ済み** （`./gradlew :projects:java:berry-books-rest:deploy`）

### ④ プロジェクトを開始するときに1回だけ実行

```bash
# 1. プロジェクトのディレクトリに移動
cd projects/react/berry-books-frontend

# 2. 依存関係をインストール（初回のみ）
npm install

# 3. 開発サーバーを起動（Vite）
npm run dev
```

> **Note**: Windowsでは**Git Bash**を使用してください。

開発サーバーは http://localhost:3000 で起動します。

> **Note**: このプロジェクトはViteを使用しています。高速なHMR（Hot Module Replacement）による開発体験を提供します。

インストール後、VSCodeを再読み込みすることをお勧めします：
- `Ctrl+Shift+P` → "Reload Window" を実行

### ⑤ プロジェクトを終了するときに1回だけ実行（CleanUp）

```bash
# 開発サーバーのターミナルで Ctrl+C を押す
```

### ⑥ アプリケーション作成・更新のたびに実行

開発中はファイルを保存すると**自動的に再読み込み**されます（HMR）。手動での再起動は不要です。

**プロダクション用ビルド:**

```bash
# プロダクション用にビルド
npm run build

# ビルド後のプレビュー（任意）
npm run preview
```

ビルドされたファイルは `dist/` ディレクトリに出力されます。

## 📍 アクセスURL

- **開発環境**: http://localhost:3000

## 🎯 プロジェクト構成

```
projects/react/berry-books-frontend/
├── src/
│   ├── components/
│   │   └── CustomerList.tsx    # 顧客一覧コンポーネント
│   ├── styles/
│   │   └── App.css             # Berry Booksテーマスタイル
│   ├── types.ts                # TypeScript型定義
│   ├── App.tsx                 # メインアプリコンポーネント
│   └── main.tsx                # エントリーポイント
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 🔧 使用している技術

- **React 18**
- **TypeScript 5**
- **Vite 5** - ビルドツール
- **CSS3** - Berry Booksテーマ

## 🎨 デザイン仕様

- **テーマカラー**: `#CF3F4E` (ストロベリーレッド)
- **サイト名**: Berry Books
- **レスポンシブデザイン**: モダンなグラデーションとシャドウ効果
- berry-books（JSF版）と同じデザインテーマを使用

## 🎯 主な機能

### 1. 顧客一覧表示
- 全顧客の情報をテーブル表示
- 顧客ID、顧客名、メールアドレス、生年月日、住所を表示

### 2. 顧客情報編集
- 各顧客行の「編集」ボタンをクリックして編集ダイアログを表示
- 顧客名、メールアドレス、生年月日、住所を編集可能
- フォームバリデーション機能搭載
- 編集後、REST API (`berry-books-rest`) に更新データを送信

### 3. 統計情報表示
- 各顧客の注文件数
- 各顧客の購入冊数（合計）

### 4. リアルタイムデータ取得
- REST API (`berry-books-rest`) からデータを取得
- ローディング状態とエラーハンドリング

## 🌐 API仕様

このフロントエンドは以下のAPIを使用します：

### 1. 顧客一覧取得
- **エンドポイント**: `GET /berry-books-rest/customers/`
- **レスポンス**: `CustomerStatsTO[]`

### 2. 顧客情報更新
- **エンドポイント**: `PUT /berry-books-rest/customers/{customerId}`
- **リクエストボディ**: 
```json
{
  "customerName": "山田太郎",
  "email": "yamada@example.com",
  "birthday": "1990-01-01",
  "address": "東京都渋谷区"
}
```
- **レスポンス**: 成功時は200 OK

### データモデル (CustomerStatsTO)

```json
{
  "customerId": 1,
  "customerName": "山田太郎",
  "email": "yamada@example.com",
  "birthday": "1990-01-01",
  "address": "東京都渋谷区",
  "orderCount": 5,
  "totalBooks": 12
}
```

## 🔄 プロキシ設定

開発環境では、Viteのプロキシ機能を使用してAPIリクエストを転送します。

```typescript
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/berry-books-rest': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

## ⚙️ 起動手順（全体）

以下は、システム全体を起動する完全な手順です。

### ① HSQLDBサーバーを起動

```bash
# プロジェクトルートで実行
./gradlew startHsqldb
```

### ② Payara Serverを起動

```bash
# プロジェクトルートで実行
./gradlew startPayara
```

### ③ データベースを初期化（初回のみ）

```bash
# プロジェクトルートで実行
./gradlew :projects:java:berry-books:setupHsqldb
```

### ④ berry-books-rest をデプロイ

```bash
# プロジェクトルートで実行
./gradlew :projects:java:berry-books-rest:war
./gradlew :projects:java:berry-books-rest:deploy
```

### ⑤ フロントエンドを起動

```bash
# berry-books-frontendディレクトリで実行
cd projects/react/berry-books-frontend
npm install  # 初回のみ
npm run dev
```

### ⑥ ブラウザでアクセス

http://localhost:3000 にアクセスして顧客一覧を確認できます。

## 🛑 アプリケーションを停止する

### 停止手順

```bash
# 1. フロントエンドを停止（開発サーバーのターミナルで Ctrl+C）

# 2. バックエンドをアンデプロイ（プロジェクトルートで実行）
./gradlew :projects:java:berry-books-rest:undeploy

# 3. Payara Serverを停止（プロジェクトルートで実行）
./gradlew stopPayara

# 4. HSQLDBサーバーを停止（プロジェクトルートで実行）
./gradlew stopHsqldb
```

## 📖 参考リンク

- [React Documentation](https://react.dev/)
- [TypeScript Documentation](https://www.typescriptlang.org/docs/)
- [Vite Documentation](https://vitejs.dev/)
- [berry-books-rest API](../java/berry-books-rest/README.md)

## 📄 ライセンス

このプロジェクトは教育目的で作成されています。

