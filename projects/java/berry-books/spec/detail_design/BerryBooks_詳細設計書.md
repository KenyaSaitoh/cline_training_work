# Berry Books アプリケーション詳細設計書

**Version:** 1.0  
**作成日:** 2025年10月25日  
**システム名:** Berry Books オンライン書店システム  
**プロジェクト名:** berry-books

---

## 目次

1. [システム概要](#1-システム概要)
2. [アーキテクチャ基本方針](#2-アーキテクチャ基本方針)
3. [技術スタック](#3-技術スタック)
4. [コンポーネント構成](#4-コンポーネント構成)
5. [認証・認可方式](#5-認証認可方式)
6. [画面遷移](#6-画面遷移)
7. [セッション管理](#7-セッション管理)
8. [ビジネスロジック](#8-ビジネスロジック)
9. [DBアクセス方式](#9-dbアクセス方式)
10. [他システム接続方式（API連携）](#10-他システム接続方式api連携)
11. [データモデル](#11-データモデル)
12. [定数・設定値](#12-定数設定値)
13. [エラーメッセージ](#13-エラーメッセージ)
14. [例外処理](#14-例外処理)
15. [トランザクション管理](#15-トランザクション管理)
16. [セキュリティ対策](#16-セキュリティ対策)
17. [デプロイメント構成](#17-デプロイメント構成)

---

## 1. システム概要

### 1.1 システムの目的

Berry Booksは、Jakarta EE 10とJSF (Jakarta Server Faces) 4.0を使用したオンライン書店のWebアプリケーションです。
ユーザーは書籍の検索、カートへの追加、注文処理などのEC機能を利用できます。

### 1.2 システムの特徴

| 項目 | 内容 |
|------|------|
| **アプリケーション形態** | Webアプリケーション（MVC型） |
| **ユーザー対象** | エンドユーザー（一般顧客） |
| **主要機能** | 書籍検索、ショッピングカート、注文処理、注文履歴参照 |
| **開発目的** | Jakarta EE学習用サンプルアプリケーション |

### 1.3 システムの制約

| 制約項目 | 内容 |
|---------|------|
| **認証方式** | 平文パスワード認証（学習用のため暗号化なし） |
| **セキュリティ** | HTTPS未使用（開発環境） |
| **スケーラビリティ** | 単一サーバー構成 |
| **ペイメント** | 決済システム未連携（受注情報の記録のみ） |

---

## 2. アーキテクチャ基本方針

### 2.1 アーキテクチャパターン

**MVC (Model-View-Controller) パターン**を採用

```
┌─────────────────────────────────────────────┐
│         Presentation Layer (View)          │
│    JSF Facelets (XHTML) + CSS + JS        │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│       Controller Layer (Managed Bean)       │
│   @Named @SessionScoped / @ViewScoped      │
│   LoginBean, CartBean, OrderBean, etc.     │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│        Business Logic Layer (Service)       │
│         @ApplicationScoped @Inject          │
│   OrderService, BookService, etc.          │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│      Data Access Layer (DAO/Repository)     │
│         @ApplicationScoped + JPA            │
│   OrderTranDao, BookDao, StockDao, etc.    │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           Persistence Layer (JPA)           │
│              @Entity + JPA 3.1              │
│   OrderTran, Book, Customer, etc.          │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│            Database (HSQLDB)                │
│          testdb (HSQLDB 2.7.x)             │
└─────────────────────────────────────────────┘
```

### 2.2 レイヤー責務定義

| レイヤー | 責務 | 実装技術 |
|---------|------|---------|
| **Presentation** | UI表示、ユーザー入力受付、バリデーション表示 | JSF Facelets (XHTML), CSS |
| **Controller** | 画面遷移制御、入力検証、Serviceへの処理委譲 | JSF Managed Bean (@Named) |
| **Service** | ビジネスロジック、トランザクション境界 | CDI Bean (@ApplicationScoped) |
| **DAO** | データアクセス、クエリ発行、CRUD操作 | JPA + EntityManager |
| **Entity** | ドメインモデル、データベーステーブルマッピング | JPA Entity (@Entity) |

### 2.3 依存性注入 (DI) 方針

- **CDI (Contexts and Dependency Injection)** を使用
- `@Inject` アノテーションによるコンストラクタインジェクション、フィールドインジェクション
- インターフェースと実装の分離は必要最小限（OrderServiceIFのみ）

### 2.4 トランザクション管理方針

- **JTA (Jakarta Transactions)** を使用
- サービス層で `@Transactional` アノテーションによる宣言的トランザクション管理
- トランザクション境界はServiceメソッド単位

---

## 3. 技術スタック

### 3.1 サーバー・ミドルウェア

| 項目 | バージョン | 用途 |
|------|-----------|------|
| **JDK** | 21 | Java実行環境 |
| **Payara Server** | 6.x | Jakarta EEアプリケーションサーバー |
| **HSQLDB** | 2.7.x | リレーショナルデータベース |
| **Gradle** | 8.x | ビルドツール |

### 3.2 Jakarta EE仕様

| 仕様 | バージョン | 用途 |
|------|-----------|------|
| **Jakarta Platform** | 10 | Java EEプラットフォーム |
| **Jakarta Server Faces (JSF)** | 4.0 | MVCフレームワーク |
| **Jakarta Persistence (JPA)** | 3.1 | O/Rマッピング |
| **Jakarta Transactions (JTA)** | 2.0 | トランザクション管理 |
| **Jakarta CDI** | 4.0 | 依存性注入 |
| **Jakarta Bean Validation** | 3.0 | バリデーション |
| **Jakarta RESTful Web Services** | 3.1 | REST API（将来対応） |
| **Jakarta Servlet** | 6.0 | サーブレット基盤 |

### 3.3 JPA実装

| 項目 | 内容 |
|------|------|
| **プロバイダー** | EclipseLink (Payaraデフォルト) |
| **データベースプラットフォーム** | `org.eclipse.persistence.platform.database.HSQLPlatform` |
| **トランザクションタイプ** | JTA |

### 3.4 フロントエンド技術

| 項目 | 内容 |
|------|------|
| **テンプレートエンジン** | JSF Facelets (XHTML) |
| **スタイルシート** | CSS3 |
| **JavaScript** | バニラJS（最小限使用） |

---

## 4. コンポーネント構成

### 4.1 パッケージ構成

```
pro.kensait.berrybooks/
├── entity/                    # JPAエンティティ（ドメインモデル）
│   ├── Book.java             # 書籍エンティティ
│   ├── Category.java         # カテゴリエンティティ
│   ├── Publisher.java        # 出版社エンティティ
│   ├── Stock.java            # 在庫エンティティ
│   ├── Customer.java         # 顧客エンティティ
│   ├── OrderTran.java        # 注文取引エンティティ
│   ├── OrderDetail.java      # 注文明細エンティティ
│   └── OrderDetailPK.java    # 注文明細複合主キー
│
├── dao/                       # データアクセス層
│   ├── BookDao.java          # 書籍DAO
│   ├── CategoryDao.java      # カテゴリDAO
│   ├── CustomerDao.java      # 顧客DAO
│   ├── StockDao.java         # 在庫DAO
│   ├── OrderTranDao.java     # 注文取引DAO
│   └── OrderDetailDao.java   # 注文明細DAO
│
├── service/                   # ビジネスロジック層
│   ├── book/
│   │   └── BookService.java  # 書籍サービス
│   ├── category/
│   │   └── CategoryService.java # カテゴリサービス
│   ├── customer/
│   │   └── CustomerService.java # 顧客サービス
│   ├── delivery/
│   │   └── DeliveryFeeService.java # 配送料金サービス
│   └── order/
│       ├── OrderService.java      # 注文サービス
│       ├── OrderServiceIF.java    # 注文サービスIF
│       ├── OrderTO.java           # 注文転送オブジェクト
│       ├── OrderHistoryTO.java    # 注文履歴TO
│       ├── OrderSummaryTO.java    # 注文サマリTO
│       └── OutOfStockException.java # 在庫不足例外
│
├── util/                      # ユーティリティ
│   └── AddressUtil.java       # 住所関連ユーティリティ
│
└── web/                       # Web層（Managed Bean）
    ├── login/
    │   └── LoginBean.java     # ログイン管理Bean
    ├── customer/
    │   └── CustomerBean.java  # 顧客情報Bean
    ├── book/
    │   ├── BookSearchBean.java # 書籍検索Bean
    │   └── SearchParam.java    # 検索パラメータ
    ├── cart/
    │   ├── CartBean.java       # カート操作Bean
    │   ├── CartSession.java    # カートセッション
    │   └── CartItem.java       # カートアイテム
    ├── order/
    │   └── OrderBean.java      # 注文処理Bean
    └── filter/
        └── AuthenticationFilter.java # 認証フィルター
```

### 4.2 主要コンポーネント一覧

#### 4.2.1 Entity層

| クラス名 | テーブル名 | 責務 |
|---------|-----------|------|
| `Book` | BOOK + STOCK | 書籍情報と在庫（SecondaryTableで結合） |
| `Category` | CATEGORY | カテゴリ情報 |
| `Publisher` | PUBLISHER | 出版社情報 |
| `Stock` | STOCK | 在庫情報（楽観的ロックを使用、学習用仕様） |
| `Customer` | CUSTOMER | 顧客情報 |
| `OrderTran` | ORDER_TRAN | 注文取引（ヘッダー） |
| `OrderDetail` | ORDER_DETAIL | 注文明細（明細行） |
| `OrderDetailPK` | - | 注文明細の複合主キー |

#### 4.2.2 DAO層

| クラス名 | 責務 | 主要メソッド |
|---------|------|------------|
| `BookDao` | 書籍データアクセス | `findById()`, `findAll()`, `queryByCategory()`, `queryByKeyword()`, `searchWithCriteria()` |
| `CategoryDao` | カテゴリデータアクセス | `findAll()`, `findById()` |
| `CustomerDao` | 顧客データアクセス | `findById()`, `findByEmail()`, `register()` |
| `StockDao` | 在庫データアクセス | `findById()`, `update()`, `findByIdWithLock()` (悲観的ロック、未使用) |
| `OrderTranDao` | 注文取引データアクセス | `findById()`, `findByCustomerId()`, `findOrderHistoryByCustomerId()`, `persist()` |
| `OrderDetailDao` | 注文明細データアクセス | `findById()`, `findByOrderTranId()`, `persist()` |

**DAO層の機能概要:**

**BookDao:**
- 書籍の検索機能を提供（主キー検索、カテゴリ検索、キーワード検索）
- 静的クエリ（JPQL）と動的クエリ（Criteria API）の両方をサポート
- カテゴリとキーワードの組み合わせ検索に対応

**StockDao:**
- 在庫情報のアクセスを提供
- 楽観的ロックによる在庫更新をサポート（学習用仕様）
- 悲観的ロック（PESSIMISTIC_WRITE）のメソッドもあるが現在は未使用
- 注文時の在庫減算処理で使用

**OrderTranDao:**
- 注文取引（ヘッダー）の永続化と検索機能を提供
- 顧客IDによる注文履歴検索（3パターン実装）
- コンストラクタ式によるパフォーマンス最適化
- FETCH JOINによる明細の一括取得

**OrderDetailDao:**
- 注文明細の永続化と検索機能を提供
- 複合主キー（OrderTranId + OrderDetailId）による検索
- 注文IDによる明細一覧取得

**CustomerDao:**
- 顧客情報のCRUD操作を提供
- メールアドレスによる一意検索（ログイン認証用）
- 顧客登録時の重複チェックに使用

**CategoryDao:**
- カテゴリマスタの参照機能を提供
- 書籍検索時のセレクトボックス用データ取得

#### 4.2.3 Service層

| クラス名 | 責務 | トランザクション |
|---------|------|----------------|
| `BookService` | 書籍ビジネスロジック | なし（参照のみ） |
| `CategoryService` | カテゴリビジネスロジック | なし（参照のみ） |
| `CustomerService` | 顧客登録・認証 | `@Transactional` |
| `OrderService` | 注文処理、注文履歴取得 | `@Transactional` |

**Service層の機能概要:**

**BookService:**
- 書籍検索ロジックの提供（カテゴリ、キーワード、複合条件）
- 静的クエリと動的クエリの切り替え
- 全書籍一覧の取得
- 書籍詳細情報の取得
- DAOを呼び出してデータを取得し、Managed Beanに返却

**CategoryService:**
- カテゴリマスタの取得
- カテゴリIDとカテゴリ名のマッピング作成（セレクトボックス用）
- カテゴリマスタデータの提供

**CustomerService:**
- 顧客登録処理（メールアドレス重複チェック含む）
- ログイン認証（メールアドレス＋パスワード）
- 顧客情報の取得
- トランザクション管理（登録時）

**DeliveryFeeService:**
- 配送料金の計算ロジック
- 配送先住所に基づく料金決定（標準：800円、沖縄県：1700円）
- 購入金額に基づく送料無料判定（5000円以上で送料無料）
- 沖縄県判定ロジック
- 送料無料対象判定ロジック

**OrderService:**
- 注文処理の中核ロジック（在庫チェック、在庫減算、注文永続化）
- 注文履歴の取得（3パターン実装：エンティティ返却、DTO返却、FETCH JOIN）
- 注文明細の取得
- 在庫不足例外のハンドリング
- 楽観的ロック例外（OptimisticLockException）のハンドリング
- トランザクション管理（注文確定時）
- 楽観的ロックによる在庫更新（学習用仕様）

#### 4.2.4 Managed Bean層

| クラス名 | スコープ | 責務 |
|---------|---------|------|
| `LoginBean` | `@SessionScoped` | ログイン・ログアウト処理、ログイン状態管理 |
| `CustomerBean` | `@SessionScoped` | 顧客情報保持、顧客登録 |
| `BookSearchBean` | `@SessionScoped` | 書籍検索、検索条件保持 |
| `CartBean` | `@SessionScoped` | カート操作（追加・削除・クリア） |
| `CartSession` | `@SessionScoped` | カート状態保持 |
| `OrderBean` | `@ViewScoped` | 注文確定処理、注文履歴表示 |

#### 4.2.5 Filter

| クラス名 | URL Pattern | 責務 |
|---------|------------|------|
| `AuthenticationFilter` | `*.xhtml` | 認証チェック、未ログインユーザーのリダイレクト |

---

## 5. 認証・認可方式

### 5.1 認証方式

**カスタム認証（平文パスワード）**

| 項目 | 内容 |
|------|------|
| **認証方式** | メールアドレス + パスワード |
| **パスワード保存形式** | 平文（※学習用のため） |
| **認証実装** | `CustomerService.authenticate()` |
| **セッション管理** | `LoginBean` (SessionScoped) |

### 5.2 認証フロー

```
┌────────────┐
│  ユーザー   │
└─────┬──────┘
      │ ①メールアドレス・パスワード入力
      ▼
┌────────────────────┐
│ index.xhtml        │
│ (ログインフォーム)   │
└─────┬──────────────┘
      │ ②processLogin()呼び出し
      ▼
┌────────────────────┐
│  LoginBean         │
│  @SessionScoped    │
└─────┬──────────────┘
      │ ③authenticate()呼び出し
      ▼
┌────────────────────┐
│ CustomerService    │
└─────┬──────────────┘
      │ ④findByEmail()
      ▼
┌────────────────────┐
│  CustomerDao       │
└─────┬──────────────┘
      │ ⑤SELECT * FROM CUSTOMER WHERE EMAIL = ?
      ▼
┌────────────────────┐
│   HSQLDB           │
└─────┬──────────────┘
      │ ⑥Customerエンティティ返却
      ▼
┌────────────────────┐
│  LoginBean         │
│  loggedIn = true   │
│  CustomerBean設定   │
└─────┬──────────────┘
      │ ⑦画面遷移
      ▼
┌────────────────────┐
│ bookSelect.xhtml   │
│ (書籍選択ページ)     │
└────────────────────┘
```

### 5.3 認可方式（アクセス制御）

**Servlet Filterによる認可制御**

| ページ | 認証要否 | 説明 |
|-------|---------|------|
| `index.xhtml` | 不要 | ログインページ |
| `customerInput.xhtml` | 不要 | 顧客登録ページ |
| `customerOutput.xhtml` | 不要 | 登録完了ページ |
| `/jakarta.faces.resource/*` | 不要 | CSS、画像などの静的リソース |
| 上記以外の全ページ | **必要** | ログイン済みユーザーのみアクセス可 |

### 5.4 ログアウト処理

- セッションを無効化（`invalidateSession()`）
- トップページ（index.xhtml）へリダイレクト

---

## 6. 画面遷移

### 6.1 画面遷移図

```
                    ┌──────────────────┐
                    │  index.xhtml     │
                    │  (ログイン)       │
                    └────┬────────┬────┘
                         │        │
                 ログイン │        │ 新規登録
                         │        │
                         │        ▼
                         │   ┌──────────────────┐
                         │   │customerInput.xhtml│
                         │   │ (顧客登録)         │
                         │   └────┬─────────────┘
                         │        │
                         │        ▼
                         │   ┌──────────────────┐
                         │   │customerOutput.xhtml│
                         │   │ (登録完了)         │
                         │   └──────────────────┘
                         │
                         ▼
          ┌──────────────────────┐
          │  bookSelect.xhtml    │  ◄──┐
          │  (書籍一覧)           │     │
          └─┬──────────────────┬─┘     │
            │                  │       │
    カート追加│          検索実行 │       │
            │                  │       │
            ▼                  ▼       │
  ┌─────────────────┐   ┌──────────────────┐
  │ cartView.xhtml  │   │ bookSearch.xhtml │
  │ (カート確認)     │   │ (書籍検索)        │
  └─┬───────────────┘   └────┬─────────────┘
    │                          │ 検索
    │注文に進む                 │
    │                          ▼
    ▼                    (bookSelect.xhtmlへ)
  ┌─────────────────┐
  │ bookOrder.xhtml │
  │ (注文確認)       │
  └─┬───────────────┘
    │
    │注文確定
    │
    ▼
  ┌─────────────────┐
  │orderSuccess.xhtml│
  │ (注文完了)       │
  └─┬───────────────┘
    │
    │注文履歴へ
    │
    ▼
  ┌─────────────────────┐
  │ orderHistory.xhtml  │  ─┐
  │ (注文履歴一覧)       │   │
  └─┬───────────────────┘   │ 3パターンあり
    │                       │
    │明細表示                │
    │                       │
    ▼                       │
  ┌─────────────────────┐   │
  │ orderDetail.xhtml   │  ─┘
  │ (注文明細詳細)       │
  └─────────────────────┘
```

### 6.2 画面一覧

| 画面ID | 画面名 | ファイル名 | Managed Bean | 認証 |
|-------|-------|----------|-------------|------|
| P001 | ログイン画面 | `index.xhtml` | `LoginBean` | 不要 |
| P002 | 顧客登録画面 | `customerInput.xhtml` | `CustomerBean` | 不要 |
| P003 | 登録完了画面 | `customerOutput.xhtml` | `CustomerBean` | 不要 |
| P004 | 書籍検索画面 | `bookSearch.xhtml` | `BookSearchBean` | 必要 |
| P005 | 書籍一覧画面 | `bookSelect.xhtml` | `BookSearchBean`, `CartBean` | 必要 |
| P006 | カート確認画面 | `cartView.xhtml` | `CartBean`, `CartSession` | 必要 |
| P007 | カートクリア完了 | `cartClear.xhtml` | `CartBean` | 必要 |
| P008 | 注文確認画面 | `bookOrder.xhtml` | `CartSession`, `OrderBean` | 必要 |
| P009 | 注文完了画面 | `orderSuccess.xhtml` | `OrderBean` | 必要 |
| P010 | 注文エラー画面 | `orderError.xhtml` | `OrderBean` | 必要 |
| P011 | 注文履歴画面（方式1） | `orderHistory.xhtml` | `OrderBean` | 必要 |
| P012 | 注文履歴画面（方式2） | `orderHistory2.xhtml` | `OrderBean` | 必要 |
| P013 | 注文履歴画面（方式3） | `orderHistory3.xhtml` | `OrderBean` | 必要 |
| P014 | 注文明細詳細画面 | `orderDetail.xhtml` | `OrderBean` | 必要 |

### 6.3 画面遷移条件

| 遷移元 | 遷移先 | トリガー | 条件 |
|-------|-------|---------|------|
| index.xhtml | bookSelect.xhtml | ログインボタン | 認証成功 |
| index.xhtml | customerInput.xhtml | 新規登録リンク | - |
| customerInput.xhtml | customerOutput.xhtml | 登録ボタン | バリデーション成功 |
| bookSearch.xhtml | bookSelect.xhtml | 検索ボタン | - |
| bookSelect.xhtml | cartView.xhtml | カートに追加ボタン | - |
| bookSelect.xhtml | bookSearch.xhtml | 検索ページリンク | - |
| cartView.xhtml | bookSelect.xhtml | 買い物を続けるリンク | - |
| cartView.xhtml | bookOrder.xhtml | 注文するボタン | カートが空でない |
| bookOrder.xhtml | orderSuccess.xhtml | 注文確定ボタン | 在庫あり、バリデーション成功 |
| bookOrder.xhtml | orderError.xhtml | 注文確定ボタン | 在庫不足 |
| orderSuccess.xhtml | orderHistory.xhtml | 注文履歴リンク | - |
| orderHistory.xhtml | orderDetail.xhtml | 明細表示リンク | - |

### 6.4 画面表示仕様

#### 6.4.1 書籍一覧画面（bookSelect.xhtml）の在庫表示ロジック

**在庫数に応じた表示制御:**

書籍一覧画面では、各書籍の在庫数に応じて以下のように表示が切り替わる

|| 在庫状態 | 在庫数条件 | 表示内容 | 操作可否 |
||---------|----------|---------|---------|
|| **在庫あり** | `book.quantity > 0` | 「買い物カゴへ」ボタンを表示 | カートへの追加可能 |
|| **在庫なし** | `book.quantity == 0` | 「入荷待ち」テキストを表示 | カートへの追加不可 |

**表示仕様:**

1. **在庫ありの場合:**
   - 「買い物カゴへ」ボタンが有効化されて表示される
   - クリックすると1冊がカートに追加される
   - ボタンのスタイルクラス：`cart-button`

2. **在庫なしの場合（入荷待ち）:**
   - 「入荷待ち」というテキストが表示される
   - ユーザーはカートに追加できない
   - テキストのスタイルクラス：`out-of-stock`（グレーアウト表示など）

**備考:**
- 在庫数は`book.quantity`フィールドで管理される（`Book`エンティティの`@SecondaryTable`で`STOCK`テーブルと結合）
- 画面表示時に`preRenderView`イベントで`BookSearchBean.refreshBookList()`が実行され、最新の在庫情報が取得される
- 在庫数は画面上の「在庫数」列にも数値として表示される

---

## 7. セッション管理

### 7.1 セッションスコープBean

| Bean | スコープ | 保持データ | ライフサイクル |
|------|---------|----------|--------------|
| `LoginBean` | `@SessionScoped` | ログイン状態、入力値 | ログイン〜ログアウト |
| `CustomerBean` | `@SessionScoped` | 顧客情報（Customer） | ログイン〜ログアウト |
| `BookSearchBean` | `@SessionScoped` | 検索条件、検索結果 | ログイン〜ログアウト |
| `CartBean` | `@SessionScoped` | カート操作用メソッド | ログイン〜ログアウト |
| `CartSession` | `@SessionScoped` | カート内容、配送情報、決済方法 | ログイン〜ログアウト |

**備考:**

現在、注文処理で楽観的ロックを使用しており、VERSION値は`CartItem`に保持され、`CartSession`を通じてセッション管理されている。

**楽観的ロック実装における注意事項:**

楽観的ロックを使用する場合、エンティティに含まれるVERSION値を画面表示から更新完了まで保持する必要がある。

**注文処理での実装:**
1. ユーザーがカートに書籍を追加 → StockエンティティからVERSION値を取得し、CartItemに保存
2. CartItemがCartSession（セッションスコープ）に保持される
3. ユーザーが注文確定ボタンをクリック → CartItem内のVERSION値でStock更新実行
4. 他のユーザーが先に在庫を更新していた場合 → `OptimisticLockException`発生

### 7.2 CartSessionデータ構造

| フィールド名 | 型 | 説明 |
|------------|---|------|
| `cartItems` | `List<CartItem>` | カートに追加された書籍のリスト（スレッドセーフ） |
| `totalPrice` | `BigDecimal` | 商品合計金額 |
| `deliveryPrice` | `BigDecimal` | 配送料金（1冊250円、4冊以上は1000円固定） |
| `deliveryAddress` | `String` | 配送先住所 |
| `settlementType` | `Integer` | 決済方法（1:銀行振込、2:クレジットカード、3:着払い） |

### 7.3 CartItemデータ構造

| フィールド名 | 型 | 説明 |
|------------|---|------|
| `bookId` | `Integer` | 書籍ID |
| `bookName` | `String` | 書籍名 |
| `publisherName` | `String` | 出版社名 |
| `price` | `BigDecimal` | 価格（注文数×単価） |
| `count` | `Integer` | 注文数 |
| `remove` | `boolean` | 削除フラグ（チェックボックス用） |
| `version` | `Long` | VERSION値（楽観的ロック用、カート追加時点の値を保持） |

### 7.4 セッションタイムアウト設定

```xml
<!-- web.xml -->
<session-config>
    <session-timeout>60</session-timeout>
    <cookie-config>
        <http-only>true</http-only>
        <secure>false</secure>
    </cookie-config>
</session-config>
```

| 項目 | 値 | 説明 |
|------|---|------|
| **タイムアウト時間** | 60分 | 無操作60分でセッション無効化 |
| **HttpOnly** | true | JavaScriptからのCookieアクセス禁止 |
| **Secure** | false | HTTP通信でもCookie送信（開発環境用） |

---

## 8. ビジネスロジック

### 8.1 書籍検索ロジック

#### 8.1.1 検索パターン

| パターン | categoryId | keyword | 実行メソッド |
|---------|-----------|---------|------------|
| 1 | あり | あり | `BookDao.query(categoryId, keyword)` |
| 2 | あり | なし | `BookDao.queryByCategory(categoryId)` |
| 3 | なし | あり | `BookDao.queryByKeyword(keyword)` |
| 4 | なし | なし | `BookDao.findAll()` |

#### 8.1.2 動的クエリ検索（Criteria API）

- CriteriaBuilderを使用して条件を動的に構築
- categoryIdとkeywordの有無に応じて条件を追加
- Predicateリストで条件を管理

### 8.2 カート管理ロジック

#### 8.2.1 書籍追加ロジック

**処理フロー:**
1. BookServiceから書籍情報を取得
2. カート内に同じ書籍が存在するかチェック
3. 存在する場合：注文数と金額を加算
4. 存在しない場合：新しいCartItemを作成しカートに追加
5. 合計金額を更新
6. cartView.xhtmlへリダイレクト

#### 8.2.2 配送料金計算ロジック

**DeliveryFeeServiceによる配送料金計算:**

**基本ルール:**
1. 購入金額が5000円以上の場合：**送料無料**
2. 購入金額が5000円未満の場合：
   - 配送先が沖縄県の場合：**1700円**
   - 上記以外の場合：**800円**（標準配送料）

**都道府県判定:**
- 配送先住所が「沖縄県」で始まる場合に沖縄県と判定
- AddressUtilによる都道府県名の検証を実施（47都道府県の正確な名称チェック）

**配送料金定数:**

| 項目 | 金額 |
|------|------|
| 標準配送料 | 800円 |
| 沖縄県配送料 | 1700円 |
| 送料無料閾値 | 5000円以上 |

### 8.3 注文処理ロジック

#### 8.3.1 注文処理フロー

```
┌────────────────────┐
│ OrderBean          │
│ placeOrder()       │
└─────┬──────────────┘
      │ OrderTO作成
      ▼
┌────────────────────┐
│ OrderService       │
│ orderBooks()       │
└─────┬──────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│ ①在庫チェック＆減算（楽観的ロック）    │
│   for each CartItem:                │
│     // カート追加時のVERSIONで      │
│     // Stockエンティティを作成      │
│     Stock stock = new Stock()       │
│     stock.setVersion(               │
│       cartItem.getVersion())        │
│     Stock current = stockDao.       │
│       findById(bookId)              │
│     if (current.quantity < count)   │
│       throw OutOfStockException     │
│     stock.setQuantity(remaining)    │
│     stockDao.update(stock)          │
│     // バージョン不一致なら          │
│     // OptimisticLockException      │
└─────┬───────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│ ②OrderTran作成＆永続化              │
│   OrderTran orderTran = new ...    │
│   orderTranDao.persist(orderTran)  │
│   em.flush() // IDを確定            │
└─────┬───────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│ ③OrderDetail作成＆永続化             │
│   for each CartItem:                │
│     OrderDetail detail = new ...   │
│     orderDetailDao.persist(detail) │
└─────┬───────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│ ④明細を含めて再取得＆返却            │
│   return orderTranDao.              │
│     findByIdWithDetails(id)         │
└─────────────────────────────────────┘
```

#### 8.3.2 在庫管理ロック戦略

**現在の実装:**

注文処理では**楽観的ロック**を使用している（学習用仕様）。通常、注文処理のような短時間トランザクションでは悲観的ロックが推奨されるが、楽観的ロックの動作を理解する目的で、あえて楽観的ロックを採用している。

カート追加から注文確定までの間に他のユーザーが在庫を更新した場合、在庫があったとしても`OptimisticLockException`が発生し、注文が失敗する。

**楽観的ロックの実装:**

|| 項目 | 内容 |
||------|------|
|| **JPA実装** | `@Version`アノテーション（Stockエンティティ） |
|| **バージョン管理カラム** | `STOCK.VERSION` (BIGINT) |
|| **VERSION保持** | `CartItem.version`フィールド → `CartSession`で管理 |
|| **競合検出** | UPDATE実行時（`WHERE VERSION = ?`が自動付加） |
|| **例外処理** | `OrderBean`で`OptimisticLockException`をキャッチ |

**処理フロー:**

1. **カート追加時**（CartBean）
   - StockエンティティからVERSION値を取得
   - CartItemに保存し、CartSession（@SessionScoped）で保持

2. **注文確定時**（OrderService）
   - CartItem内のVERSION値でStockエンティティを作成
   - 在庫更新を実行（JPAが自動的にバージョンチェック）
   - バージョン不一致の場合は`OptimisticLockException`

3. **例外処理**（OrderBean）
   - 「他のユーザーが同時に注文しました。もう一度お試しください」とエラー表示

### 8.4 注文履歴取得ロジック

#### 8.4.1 注文履歴取得パターン

| 方式 | メソッド | 戻り値 | 特徴 |
|-----|---------|-------|------|
| **方式1** | `getOrderHistory()` | `List<OrderTran>` | エンティティをそのまま返却 |
| **方式2** | `getOrderHistory2()` | `List<OrderHistoryTO>` | DTOで明細レベルの詳細データ返却 |
| **方式3** | `getOrderHistory3()` | `List<OrderTran>` | FETCH JOINで明細を含めて取得 |

#### 8.4.2 方式2の特徴（DTO使用）

- コンストラクター式を使用してDTOを直接生成
- 必要なフィールドのみを取得（パフォーマンス最適化）
- INNER JOINで注文、明細、書籍、出版社を結合
- 注文日、注文ID、明細ID、書籍名、出版社名、価格、数量を返却

---

## 9. DBアクセス方式

### 9.1 JPA設定

#### 9.1.1 persistence.xml

| 項目 | 値 | 説明 |
|------|---|------|
| **PersistenceUnit名** | `bookstorePU` | EntityManagerの識別名 |
| **トランザクションタイプ** | `JTA` | コンテナ管理トランザクション |
| **データソース** | `jdbc/HsqldbDS` | JNDI名 |
| **DBプラットフォーム** | `HSQLPlatform` | HSQLDB用SQLダイアレクト |
| **ログレベル** | `FINE` | 詳細ログ出力 |

### 9.2 データソース設定

| 項目 | 値 |
|------|---|
| **JNDI名** | `jdbc/HsqldbDS` |
| **接続プール名** | `HsqldbPool` |
| **データベースURL** | `jdbc:hsqldb:hsql://localhost:9001/testdb` |
| **ドライバークラス** | `org.hsqldb.jdbcDriver` |
| **ユーザー名** | `SA` |
| **パスワード** | （空文字） |

### 9.3 エンティティマッピング戦略

#### 9.3.1 主キー生成戦略

| テーブル | 主キー | 生成戦略 |
|---------|-------|---------|
| BOOK | BOOK_ID | `IDENTITY` (自動採番) |
| CATEGORY | CATEGORY_ID | `IDENTITY` (自動採番) |
| PUBLISHER | PUBLISHER_ID | `IDENTITY` (自動採番) |
| CUSTOMER | CUSTOMER_ID | `IDENTITY` (自動採番) |
| ORDER_TRAN | ORDER_TRAN_ID | `IDENTITY` (自動採番) |
| ORDER_DETAIL | (ORDER_TRAN_ID, ORDER_DETAIL_ID) | 複合主キー（アプリで採番） |
| STOCK | BOOK_ID | 手動設定（BOOKと同じID） |

#### 9.3.2 リレーションシップマッピング

| エンティティ1 | 関係 | エンティティ2 | マッピング方法 |
|------------|------|-------------|--------------|
| Book | N:1 | Category | `@ManyToOne` + `@JoinColumn` |
| Book | N:1 | Publisher | `@ManyToOne` + `@JoinColumn` |
| Book | 1:1 | Stock | `@SecondaryTable` |
| OrderTran | 1:N | OrderDetail | `@OneToMany` (mappedBy) |
| OrderDetail | N:1 | OrderTran | `@ManyToOne` (insertable=false, updatable=false) |
| OrderDetail | N:1 | Book | `@ManyToOne` + `@JoinColumn` |

#### 9.3.3 複合主キーマッピング

**OrderDetailPKクラス:**
- `orderTranId`と`orderDetailId`の2つのフィールド
- `equals()`と`hashCode()`の実装必須

**OrderDetailエンティティ:**
- `@IdClass(OrderDetailPK.class)`で複合主キー指定
- 両フィールドに`@Id`アノテーション付与

### 9.4 クエリ実装パターン

#### 9.4.1 JPQL（静的クエリ）

- TypedQueryを使用
- パラメータバインディング（`:paramName`形式）
- SQLインジェクション対策

#### 9.4.2 Criteria API（動的クエリ）

- CriteriaBuilder、CriteriaQuery、Rootを使用
- Predicateリストで条件を動的に構築
- 条件の有無に応じてクエリを組み立て

#### 9.4.3 コンストラクター式

- `SELECT new パッケージ名.DTO名(...)`形式
- 必要なフィールドのみを取得
- パフォーマンス最適化

### 9.5 フェッチ戦略

| エンティティ | リレーション | フェッチタイプ | 理由 |
|------------|------------|-------------|------|
| Book → Category | `@ManyToOne` | `EAGER`（デフォルト） | カテゴリ情報は常に必要 |
| Book → Publisher | `@ManyToOne` | `EAGER`（デフォルト） | 出版社情報は常に必要 |
| OrderTran → OrderDetail | `@OneToMany` | `EAGER`（明示指定） | 明細は常に必要 |
| OrderDetail → Book | `@ManyToOne` | `EAGER`（デフォルト） | 書籍情報は常に必要 |

---

## 10. 他システム接続方式（API連携）

### 10.1 外部システム連携概要

**現時点では外部システムとのAPI連携は実装していません。**

### 10.2 将来の連携候補

| システム | 連携内容 | 方式 |
|---------|---------|------|
| **顧客管理システム** | 顧客情報の同期 | REST API (berry-books-rest) |
| **決済システム** | クレジットカード決済 | REST API / WebAPI |
| **在庫管理システム** | リアルタイム在庫連携 | メッセージング (JMS) |
| **配送システム** | 配送状況追跡 | REST API |

### 10.3 設定ファイル

```properties
# config.properties
customer.api.base-url = http://localhost:8081/customers
```

---

## 11. データモデル

### 11.1 ER図

```
┌──────────────┐
│  PUBLISHER   │
│──────────────│
│ PUBLISHER_ID │◄──┐
│ PUBLISHER_NAME│  │
└──────────────┘  │
                   │
┌──────────────┐  │      ┌──────────────┐
│  CATEGORY    │  │      │   STOCK      │
│──────────────│  │      │──────────────│
│ CATEGORY_ID  │◄─┐  ┌──►│ BOOK_ID  (PK)│
│ CATEGORY_NAME│  │  │   │ QUANTITY     │
└──────────────┘  │  │   │ VERSION      │
                   │  │   └──────────────┘
         ┌─────────┴──┴─────────┐
         │        BOOK          │
         │──────────────────────│
         │ BOOK_ID (PK)         │
         │ BOOK_NAME            │
         │ AUTHOR               │
         │ CATEGORY_ID (FK)     │
         │ PUBLISHER_ID (FK)    │
         │ PRICE                │
         └──────────┬───────────┘
                    │
                    │
         ┌──────────▼───────────┐
         │    ORDER_DETAIL      │
         │──────────────────────│
         │ ORDER_TRAN_ID (PK,FK)│
         │ ORDER_DETAIL_ID (PK) │
         │ BOOK_ID (FK)         │◄───┐
         │ PRICE                │    │
         │ COUNT                │    │
         └──────────┬───────────┘    │
                    │                │
         ┌──────────▼───────────┐    │
         │    ORDER_TRAN        │    │
         │──────────────────────│    │
         │ ORDER_TRAN_ID (PK)   │    │
         │ ORDER_DATE           │    │
         │ CUSTOMER_ID (FK)     │    │
         │ TOTAL_PRICE          │    │
         │ DELIVERY_PRICE       │    │
         │ DELIVERY_ADDRESS     │    │
         │ SETTLEMENT_TYPE      │    │
         └──────────┬───────────┘    │
                    │                │
         ┌──────────▼───────────┐    │
         │      CUSTOMER        │    │
         │──────────────────────│    │
         │ CUSTOMER_ID (PK)     │    │
         │ CUSTOMER_NAME        │    │
         │ EMAIL                │    │
         │ PASSWORD             │    │
         │ BIRTHDAY             │    │
         │ ADDRESS              │    │
         └──────────────────────┘    │
```

### 11.2 テーブル定義

#### 11.2.1 PUBLISHER（出版社）

| カラム名 | 型 | NULL | デフォルト | 制約 | 説明 |
|---------|---|------|----------|------|------|
| PUBLISHER_ID | INTEGER | NOT NULL | IDENTITY | PK | 出版社ID（自動採番） |
| PUBLISHER_NAME | VARCHAR(30) | NOT NULL | - | - | 出版社名 |

**主キー:** `PUBLISHER_ID`

#### 11.2.2 CATEGORY（カテゴリ）

| カラム名 | 型 | NULL | デフォルト | 制約 | 説明 |
|---------|---|------|----------|------|------|
| CATEGORY_ID | INTEGER | NOT NULL | IDENTITY | PK | カテゴリID（自動採番） |
| CATEGORY_NAME | VARCHAR(20) | NOT NULL | - | - | カテゴリ名 |

**主キー:** `CATEGORY_ID`

#### 11.2.3 BOOK（書籍）

| カラム名 | 型 | NULL | デフォルト | 制約 | 説明 |
|---------|---|------|----------|------|------|
| BOOK_ID | INTEGER | NOT NULL | IDENTITY | PK | 書籍ID（自動採番） |
| BOOK_NAME | VARCHAR(80) | NOT NULL | - | - | 書籍名 |
| AUTHOR | VARCHAR(40) | NOT NULL | - | - | 著者名 |
| CATEGORY_ID | INT | NOT NULL | - | FK | カテゴリID |
| PUBLISHER_ID | INT | NOT NULL | - | FK | 出版社ID |
| PRICE | INT | NOT NULL | - | - | 価格（円） |

**主キー:** `BOOK_ID`

**外部キー:**
- `FK_CATEGORY_ID`: CATEGORY(CATEGORY_ID)
- `FK_PUBLISHER_ID`: PUBLISHER(PUBLISHER_ID)

#### 11.2.4 STOCK（在庫）

| カラム名 | 型 | NULL | デフォルト | 制約 | 説明 |
|---------|---|------|----------|------|------|
| BOOK_ID | INT | NOT NULL | - | PK | 書籍ID |
| QUANTITY | INT | NOT NULL | - | - | 在庫数 |
| VERSION | BIGINT | NOT NULL | - | - | 楽観的ロック用バージョン番号 |

**主キー:** `BOOK_ID`

**備考:** BOOKテーブルと1:1関係

#### 11.2.5 CUSTOMER（顧客）

| カラム名 | 型 | NULL | デフォルト | 制約 | 説明 |
|---------|---|------|----------|------|------|
| CUSTOMER_ID | INTEGER | NOT NULL | IDENTITY | PK | 顧客ID（自動採番） |
| CUSTOMER_NAME | VARCHAR(50) | NOT NULL | - | - | 顧客名 |
| EMAIL | VARCHAR(100) | NOT NULL | - | UNIQUE | メールアドレス |
| PASSWORD | VARCHAR(100) | NOT NULL | - | - | パスワード（平文） |
| BIRTHDAY | DATE | NULL | - | - | 生年月日 |
| ADDRESS | VARCHAR(200) | NULL | - | - | 住所 |

**主キー:** `CUSTOMER_ID`

**インデックス:** EMAIL（UNIQUE制約）

#### 11.2.6 ORDER_TRAN（注文取引）

| カラム名 | 型 | NULL | デフォルト | 制約 | 説明 |
|---------|---|------|----------|------|------|
| ORDER_TRAN_ID | INTEGER | NOT NULL | IDENTITY | PK | 注文取引ID（自動採番） |
| ORDER_DATE | DATE | NOT NULL | - | - | 注文日 |
| CUSTOMER_ID | INT | NOT NULL | - | FK | 顧客ID |
| TOTAL_PRICE | INT | NOT NULL | - | - | 注文金額合計（円） |
| DELIVERY_PRICE | INT | NOT NULL | - | - | 配送料金（円） |
| DELIVERY_ADDRESS | VARCHAR(30) | NOT NULL | - | - | 配送先住所 |
| SETTLEMENT_TYPE | INT | NOT NULL | - | - | 決済方法（1:銀行振込、2:クレジットカード、3:着払い） |

**主キー:** `ORDER_TRAN_ID`

**外部キー:**
- `FK_CUSTOMER_ID`: CUSTOMER(CUSTOMER_ID)

#### 11.2.7 ORDER_DETAIL（注文明細）

| カラム名 | 型 | NULL | デフォルト | 制約 | 説明 |
|---------|---|------|----------|------|------|
| ORDER_TRAN_ID | INT | NOT NULL | - | PK, FK | 注文取引ID |
| ORDER_DETAIL_ID | INT | NOT NULL | - | PK | 注文明細ID（アプリ採番） |
| BOOK_ID | INT | NOT NULL | - | FK | 書籍ID |
| PRICE | INT | NOT NULL | - | - | 購入時点の価格（円） |
| COUNT | INT | NOT NULL | - | - | 注文数 |

**主キー:** `(ORDER_TRAN_ID, ORDER_DETAIL_ID)`

**外部キー:**
- `FK_ORDER_TRAN_ID`: ORDER_TRAN(ORDER_TRAN_ID)
- `FK_BOOK_ID`: BOOK(BOOK_ID)

### 11.3 マスタデータ

#### 11.3.1 PUBLISHER（出版社）

| PUBLISHER_ID | PUBLISHER_NAME |
|-------------|----------------|
| 1 | デジタルフロンティア出版 |
| 2 | コードブレイクプレス |
| 3 | ネットワークノード出版 |
| 4 | クラウドキャスティング社 |
| 5 | データドリフト社 |

#### 11.3.2 CATEGORY（カテゴリ）

| CATEGORY_ID | CATEGORY_NAME |
|------------|--------------|
| 1 | Java |
| 2 | SpringBoot |
| 3 | SQL |
| 4 | HTML/CSS |
| 5 | JavaScript |
| 6 | Python |
| 7 | 生成AI |
| 8 | クラウド |
| 9 | AWS |

#### 11.3.3 BOOK（書籍）

全50冊のデータが登録されています。以下は抜粋：

| BOOK_ID | BOOK_NAME | AUTHOR | CATEGORY_ID | PUBLISHER_ID | PRICE |
|---------|-----------|--------|-------------|-------------|-------|
| 1 | Java SEディープダイブ | Michael Johnson | 1 | 3 | 3400 |
| 2 | JVMとバイトコードの探求 | James Lopez | 1 | 1 | 4200 |
| 6 | Jakarta EE究極テストガイド | Thomas Rodriguez | 1 | 4 | 5200 |
| 9 | SpringBoot in Cloud | Paul Martin | 2 | 3 | 3000 |
| 14 | データベースの科学 | Mark Jackson | 3 | 4 | 2500 |
| 23 | JavaScriptマジック | Adam Wright | 5 | 4 | 2800 |
| 30 | Pythonプログラミング実践入門 | Alice Carter | 6 | 2 | 3000 |
| 36 | 生成AIシステム設計ガイド | Fiona Walker | 7 | 2 | 4200 |
| 42 | クラウドアーキテクチャ実践パターン | Kevin Anderson | 8 | 1 | 3900 |
| 46 | AWS設計原則とベストプラクティス | Oliver Ramirez | 9 | 5 | 4200 |

### 11.4 データ整合性制約

| 制約種別 | テーブル | 制約内容 |
|---------|---------|---------|
| **外部キー制約** | BOOK | CATEGORY_IDはCATEGORY.CATEGORY_IDに存在 |
| **外部キー制約** | BOOK | PUBLISHER_IDはPUBLISHER.PUBLISHER_IDに存在 |
| **外部キー制約** | ORDER_TRAN | CUSTOMER_IDはCUSTOMER.CUSTOMER_IDに存在 |
| **外部キー制約** | ORDER_DETAIL | ORDER_TRAN_IDはORDER_TRAN.ORDER_TRAN_IDに存在 |
| **外部キー制約** | ORDER_DETAIL | BOOK_IDはBOOK.BOOK_IDに存在 |
| **一意制約** | CUSTOMER | EMAILは一意 |
| **NOT NULL制約** | 全テーブル | 主キーはNOT NULL |

---

## 12. 定数・設定値

### 12.1 決済方法コード

| コード | 名称 |
|-------|------|
| 1 | 銀行振り込み |
| 2 | クレジットカード |
| 3 | 着払い |

### 12.2 配送料金計算ロジック定数

| 項目 | 値 |
|------|---|
| 標準配送料 | 800円 |
| 沖縄県配送料 | 1700円 |
| 送料無料閾値 | 5000円以上 |

### 12.3 アプリケーション設定

#### 12.3.1 web.xml設定値

| 設定項目 | 設定値 | 説明 |
|---------|-------|------|
| `jakarta.faces.PROJECT_STAGE` | `Development` | JSF実行ステージ（開発モード） |
| `jakarta.faces.FACELETS_SKIP_COMMENTS` | `true` | Faceletsコメント除去 |
| `jakarta.faces.STATE_SAVING_METHOD` | `server` | ビューステートをサーバー側に保存 |
| `session-timeout` | `60` | セッションタイムアウト（分） |

#### 12.3.2 persistence.xml設定値

| 設定項目 | 設定値 | 説明 |
|---------|-------|------|
| `eclipselink.target-database` | `HSQLPlatform` | DBプラットフォーム |
| `eclipselink.logging.level` | `FINE` | ログレベル（詳細） |
| `eclipselink.jdbc.user` | `SA` | DBユーザー名 |
| `eclipselink.jdbc.password` | （空文字） | DBパスワード |

#### 12.3.3 config.properties

| プロパティキー | 値 | 説明 |
|-------------|---|------|
| `customer.api.base-url` | `http://localhost:8081/customers` | 顧客APIのベースURL（将来用） |

---

## 13. エラーメッセージ

### 13.1 メッセージプロパティファイル

**ファイル:** `src/main/resources/messages.properties`

| メッセージキー | メッセージ内容 |
|-------------|-------------|
| `typeMismatch.int` | 数値を入力してください |
| `typeMismatch.java.time.LocalDate` | yyyy/M/d形式で入力してください |
| `error.address.prefecture` | 都道府県名が正しく入力されていません（47都道府県で始まる必要あり） |
| `error.customer.exists` | すでに指定されたメールアドレスは登録されています |
| `error.email.not-exist` | メールアドレスが存在しません |
| `error.password.unmatch` | 指定されたパスワードが間違っているようです |
| `error.cart.empty` | カートに商品が一つも入っていません |
| `error.order.outof-stock` | 注文された書籍「{0}」は、指定された個数、在庫に存在しません |
| `error.order.optimistic-lock` | 別の顧客によって在庫が更新されました |

### 13.2 画面表示メッセージ

#### 13.2.1 ログイン失敗

| メッセージ種別 | メッセージ |
|-------------|----------|
| SEVERITY | ERROR |
| サマリー | ログインに失敗しました |
| 詳細 | メールアドレスまたはパスワードが正しくありません |

#### 13.2.2 カート空エラー

| メッセージ種別 | メッセージ |
|-------------|----------|
| SEVERITY | WARN |
| 内容 | カートに商品が入っていません |

#### 13.2.3 在庫不足エラー

| メッセージ種別 | メッセージ |
|-------------|----------|
| SEVERITY | ERROR |
| 内容 | 注文された書籍「{書籍名}」の在庫が不足しています |

#### 13.2.4 住所検証エラー

| メッセージ種別 | メッセージ |
|-------------|----------|
| SEVERITY | ERROR |
| 内容 | 都道府県名が正しく入力されていません |

**検証方式:**
- **AddressUtilクラス**による手動バリデーション
- 47都道府県の正式名称で始まるかをチェック
  - 都道府県：北海道、東京都、京都府、大阪府、〇〇県（43県）
- 顧客登録時および注文時の配送先住所で実施
- Bean Validationではなく、サービス層またはManaged Bean層で明示的にチェック

---

## 14. 例外処理

### 14.1 例外クラス階層

```
java.lang.RuntimeException
    └── pro.kensait.berrybooks.service.order.OutOfStockException
```

### 14.2 業務例外

#### 14.2.1 OutOfStockException（在庫不足例外）

**パッケージ:** `pro.kensait.berrybooks.service.order`

**継承元:** `RuntimeException`

**発生条件:** 注文数が在庫数を超えた場合

**フィールド:**

| フィールド名 | 型 | 説明 |
|------------|---|------|
| `bookId` | `Integer` | 在庫不足の書籍ID |
| `bookName` | `String` | 在庫不足の書籍名 |

**使用箇所:** OrderService.orderBooks()メソッド内

**例外ハンドリング:** OrderBean.placeOrder()メソッドでキャッチし、orderError.xhtmlへ遷移

### 14.3 システム例外

| 例外クラス | 発生条件 | ハンドリング方法 |
|----------|---------|----------------|
| `IllegalArgumentException` | メールアドレス重複（顧客登録時） | CustomerServiceでスロー、画面でキャッチしてメッセージ表示 |
| `RuntimeException` | 注文・注文明細が見つからない | ServiceでRuntimeExceptionスロー |
| `PersistenceException` | DB接続エラー、SQL実行エラー | トランザクションロールバック、画面でエラーメッセージ表示 |
| `OptimisticLockException` | 楽観的ロック衝突 | トランザクションロールバック、再試行促進メッセージ |

### 14.4 例外処理フロー

```
┌─────────────────┐
│  Managed Bean   │
│  (OrderBean)    │
└────────┬────────┘
         │ try {
         ▼
┌─────────────────┐
│    Service      │
│ (OrderService)  │
└────────┬────────┘
         │
         │ 在庫不足検出
         │ throw OutOfStockException
         │
         ▼
┌─────────────────┐
│  Managed Bean   │
│  } catch (Out   │
│  OfStockExcep   │
│  tion e) {      │
└────────┬────────┘
         │
         │ エラー情報設定
         │
         ▼
┌─────────────────┐
│ orderError.xhtml│
│ (エラー画面)     │
└─────────────────┘
```

---

## 15. トランザクション管理

### 15.1 トランザクション境界

**トランザクション境界:** Serviceメソッド単位

**実装方式:** `@Transactional` アノテーション（宣言的トランザクション）

### 15.2 トランザクション属性

| クラス | メソッド | トランザクション属性 | 説明 |
|-------|---------|-------------------|------|
| `OrderService` | `orderBooks()` | `REQUIRED` | トランザクション必須（新規開始または既存参加） |
| `CustomerService` | `registerCustomer()` | `REQUIRED` | トランザクション必須 |
| `BookService` | `getBook()` | なし | 参照のみ、トランザクション不要 |

### 15.3 ロールバック条件

| 条件 | ロールバック契機 |
|------|---------------|
| `OutOfStockException` スロー | 自動ロールバック（RuntimeException） |
| `OptimisticLockException` スロー | 自動ロールバック（楽観的ロック競合） |
| `IllegalArgumentException` スロー | 自動ロールバック（RuntimeException） |
| `PersistenceException` スロー | 自動ロールバック |
| メソッド正常終了 | コミット |

### 15.4 トランザクション分離レベル

| 項目 | 値 | 説明 |
|------|---|------|
| **分離レベル** | `READ_COMMITTED` | HSQLDBデフォルト |

**備考:**
- トランザクション分離レベルは`READ_COMMITTED`であり、コミット済みのデータのみを読み取る
- 在庫更新時のロック戦略（悲観的ロック/楽観的ロック）については、「8.3.2 在庫管理ロック戦略」を参照

---

## 16. セキュリティ対策

### 16.1 認証セキュリティ

| 項目 | 実装状況 | 内容 |
|------|---------|------|
| **パスワード暗号化** | ❌ 未実装 | 平文保存（学習用のため） |
| **HTTPS通信** | ❌ 未実装 | HTTP通信（開発環境） |
| **セッション固定攻撃対策** | ✅ 実装済み | ログイン後にセッションID再生成（Payara標準機能） |
| **セッションタイムアウト** | ✅ 実装済み | 60分 |

### 16.2 Cookieセキュリティ

| 項目 | 設定値 | 効果 |
|------|-------|------|
| **HttpOnly** | `true` | JavaScriptからのCookieアクセスを禁止（XSS対策） |
| **Secure** | `false` | HTTP通信でもCookieを送信（開発環境用、本番では`true`にすべき） |

### 16.3 SQLインジェクション対策

**対策:** JPA/JPQLによるパラメータバインディング

### 16.4 XSS対策

**対策:** JSFの自動エスケープ機能

### 16.5 CSRF対策

**対策:** JSFの自動CSRF保護機能（ViewStateトークン）

### 16.6 認可制御

**対策:** Servlet Filterによるアクセス制御

### 16.7 セキュリティ改善推奨事項（本番環境向け）

| 項目 | 現状 | 推奨 |
|------|-----|------|
| **パスワード保存** | 平文 | bcrypt/PBKDF2などでハッシュ化 |
| **通信プロトコル** | HTTP | HTTPS必須 |
| **Secure Cookie** | false | true（HTTPS環境では必須） |
| **セッションタイムアウト** | 60分 | 用途に応じて調整（15-30分推奨） |
| **アカウントロック** | なし | ログイン失敗N回でロック |
| **監査ログ** | なし | ログイン・注文などの重要操作をログ記録 |

---

## 17. デプロイメント構成

### 17.1 システム構成図

```
┌─────────────────────────────────────────┐
│          クライアント（Webブラウザ）       │
│           Chrome / Edge / Firefox       │
└──────────────┬──────────────────────────┘
               │ HTTP (Port 8080)
               ▼
┌─────────────────────────────────────────┐
│         Payara Server 6                 │
│   ┌─────────────────────────────────┐   │
│   │  berry-books.war                │   │
│   │  (Webアプリケーション)            │   │
│   │  - JSF 4.0                      │   │
│   │  - JPA 3.1 (EclipseLink)        │   │
│   │  - CDI 4.0                      │   │
│   └──────────────┬──────────────────┘   │
│                  │ JNDI: jdbc/HsqldbDS  │
│   ┌──────────────▼──────────────────┐   │
│   │  Connection Pool: HsqldbPool    │   │
│   │  (JDBC Driver: org.hsqldb)      │   │
│   └──────────────┬──────────────────┘   │
└──────────────────┼──────────────────────┘
                   │ JDBC (Port 9001)
                   ▼
┌─────────────────────────────────────────┐
│         HSQLDB Server 2.7.x             │
│   Database: testdb                      │
│   TCP Server Mode (Port 9001)           │
└─────────────────────────────────────────┘
```

### 17.2 デプロイメント環境

| 項目 | 値 |
|------|---|
| **OS** | Windows 10 / Linux / macOS |
| **JDK** | OpenJDK 21 |
| **アプリケーションサーバー** | Payara Server 6.x |
| **データベース** | HSQLDB 2.7.x (TCP Server Mode) |
| **ビルドツール** | Gradle 8.x |

### 17.3 デプロイ手順

#### 17.3.1 初回セットアップ

```powershell
# 1. データベーステーブルとデータを作成
.\gradlew :projects:berry-books:setupHsqldb

# 2. プロジェクトをビルド
.\gradlew :projects:berry-books:war

# 3. プロジェクトをデプロイ
.\gradlew :projects:berry-books:deploy
```

#### 17.3.2 更新デプロイ

```powershell
# アプリケーションを再ビルドして再デプロイ
.\gradlew :projects:berry-books:war
.\gradlew :projects:berry-books:deploy
```

#### 17.3.3 アンデプロイ

```powershell
# プロジェクトをアンデプロイ
.\gradlew :projects:berry-books:undeploy
```

### 17.4 アクセスURL

| 種別 | URL |
|------|-----|
| **アプリケーショントップ** | http://localhost:8080/berry-books |
| **Payara管理コンソール** | http://localhost:4848 |

### 17.5 ファイル配置

| 種別 | パス |
|------|------|
| **WARファイル** | `projects/berry-books/build/libs/berry-books.war` |
| **デプロイ先** | `payara6/glassfish/domains/domain1/applications/berry-books/` |
| **ログファイル** | `payara6/glassfish/domains/domain1/logs/server.log` |
| **HSQLDBデータ** | `hsqldb/data/testdb.*` |

---

## 付録A: Gradleタスク一覧

| タスク | 説明 |
|-------|------|
| `war` | WARファイルをビルド |
| `deploy` | Payara Serverにデプロイ |
| `undeploy` | Payara Serverからアンデプロイ |
| `setupHsqldb` | データベース初期化（テーブル作成＋データ投入） |

---

## 付録B: 参考文献・リンク

| 項目 | URL |
|------|-----|
| **Jakarta EE 10 Platform** | https://jakarta.ee/specifications/platform/10/ |
| **Jakarta Server Faces 4.0** | https://jakarta.ee/specifications/faces/4.0/ |
| **Jakarta Persistence 3.1** | https://jakarta.ee/specifications/persistence/3.1/ |
| **Payara Server Documentation** | https://docs.payara.fish/ |
| **HSQLDB Documentation** | http://hsqldb.org/doc/2.0/guide/ |

---

**以上**

