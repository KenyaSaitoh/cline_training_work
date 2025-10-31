# 売上トランザクションエクスポート仕様書

## 1. 概要

売上システムから会計システムへの売上トランザクションデータのエクスポート仕様です。受注・出荷・請求・入金に関わるトランザクションを会計仕訳に変換します。

### ドキュメント管理

| 項目 | 内容 |
|------|------|
| ドキュメント名 | 売上トランザクションエクスポート詳細設計書 |
| バージョン | 3.0 |
| 作成日 | 2025-10-26 |
| 更新日 | 2025-10-29 |
| ステータス | 承認済み |

### 改訂履歴

| バージョン | 日付 | 内容 |
|-----------|------|------|
| 3.0 | 2025-10-29 | Python Native/PySpark両対応、統合出力対応 |
| 2.0 | 2025-10-26 | CSV入出力ベースへ更新 |
| 1.0 | 2025-10-25 | 初版作成 |

---

## 2. 入力データ: sales_txn_export.csv

### 2.1 ファイル情報

| 項目 | 内容 |
|------|------|
| ファイル名 | sales_txn_export.csv |
| ファイルパス | test_data/sales/sales_txn_export.csv |
| 文字コード | UTF-8 |
| 形式 | CSV（ヘッダー行あり） |
| 区切り文字 | カンマ（,） |

### 2.2 基本項目

| 項目名 | データ型 | 必須 | 説明 | 値の例 |
|--------|---------|------|------|--------|
| export_id | INTEGER | ✔ | エクスポートID（主キー） | 1 |
| batch_id | VARCHAR(40) | ✔ | バッチID | BATCH_20251026_001 |
| source_txn_id | VARCHAR(40) | ✔ | 原票トランザクションID | TXN000001 |
| source_line_id | VARCHAR(40) | ✔ | 原票明細ID | LINE000001 |
| txn_type | VARCHAR(20) | ✔ | トランザクションタイプ | INVOICE, SHIP, ORDER, CM, PMT |
| txn_status | VARCHAR(20) | ✔ | トランザクションステータス | OPEN, CLOSED, CANCELLED |
| event_timestamp | TIMESTAMP | ✔ | イベント発生日時 | 2025-10-26 12:30:00 |
| update_timestamp | TIMESTAMP | ✔ | 更新タイムスタンプ | 2025-10-26 15:00:00 |

### 2.3 伝票情報

| 項目名 | データ型 | 必須 | 説明 | 値の例 |
|--------|---------|------|------|--------|
| order_id | VARCHAR(40) | - | 受注ID | ORD000001 |
| order_date | DATE | - | 受注日 | 2025-10-20 |
| invoice_id | VARCHAR(40) | - | 請求ID | INV000001 |
| invoice_date | DATE | - | 請求日 | 2025-10-26 |
| shipment_id | VARCHAR(40) | - | 出荷ID | SHIP000001 |
| shipment_date | DATE | - | 出荷日 | 2025-10-25 |

### 2.4 顧客・営業情報

| 項目名 | データ型 | 必須 | 説明 | 値の例 |
|--------|---------|------|------|--------|
| customer_code | VARCHAR(40) | ✔ | 顧客コード | CUST0001 |
| bill_to_site | VARCHAR(60) | - | 請求先サイト | 東京本社 |
| ship_to_site | VARCHAR(60) | - | 出荷先サイト | 大阪支店 |
| salesperson_code | VARCHAR(30) | - | 営業担当者コード | SALES01 |
| campaign_code | VARCHAR(30) | - | キャンペーンコード | CAMP2025Q4 |

### 2.5 商品・数量情報

| 項目名 | データ型 | 必須 | 説明 | 値の例 |
|--------|---------|------|------|--------|
| product_code | VARCHAR(40) | ✔ | 商品コード | PROD0001 |
| product_name | VARCHAR(200) | - | 商品名 | ノートPC ThinkPad X1 |
| quantity_ordered | DECIMAL(19,6) | - | 受注数量 | 10.000000 |
| quantity_shipped | DECIMAL(19,6) | - | 出荷数量 | 10.000000 |
| quantity_invoiced | DECIMAL(19,6) | - | 請求数量 | 10.000000 |
| unit_of_measure | VARCHAR(10) | - | 単位 | EA, KG, L |

### 2.6 金額・税情報

| 項目名 | データ型 | 必須 | 説明 | 値の例 |
|--------|---------|------|------|--------|
| unit_price | DECIMAL(19,2) | ✔ | 単価 | 150000.00 |
| line_amount | DECIMAL(19,2) | - | 明細金額 | 1500000.00 |
| currency_code | VARCHAR(3) | ✔ | 通貨コード | JPY, USD, EUR |
| exchange_rate | DECIMAL(19,6) | - | 為替レート | 150.500000 |
| tax_code | VARCHAR(20) | ✔ | 税コード | TAX_STANDARD, TAX_REDUCED |
| tax_rate | DECIMAL(7,6) | ✔ | 税率 | 0.100000 |
| tax_amount | DECIMAL(19,2) | - | 税額 | 150000.00 |

### 2.7 参照情報

| 項目名 | データ型 | 必須 | 説明 | 値の例 |
|--------|---------|------|------|--------|
| reference1 | VARCHAR(100) | - | 参照1 | 案件番号: PRJ2025-001 |
| reference2 | VARCHAR(100) | - | 参照2 | 契約番号: CNT2025-0050 |
| remarks | VARCHAR(500) | - | 備考 | 初回取引、要与信確認 |
| reversal_flag | BOOLEAN | - | 取消フラグ | FALSE |

---

## 3. 出力データ: accounting_txn_interface.csv

### 3.1 ファイル情報

| 項目 | 内容 |
|------|------|
| ファイル名 | accounting_txn_interface.csv（統合ファイル） |
| ファイルパス | output/accounting_txn_interface.csv |
| 文字コード | UTF-8 |
| 形式 | CSV（ヘッダー行あり） |
| 区切り文字 | カンマ（,） |
| 備考 | 売上・人事・在庫の全データを含む統合ファイル |

### 3.2 売上データ出力項目

**会計インターフェースへの変換仕様**

| 出力項目 | 入力項目/計算式 | 説明 |
|---------|---------------|------|
| batch_id | システム生成 | 形式: SALE_YYYYMMDD_HHMMSS |
| source_system | 固定値: 'SALE' | ソースシステムコード |
| source_doc_type | txn_type | INVOICE, SHIP, ORDER等 |
| source_doc_id | source_txn_id | 原票トランザクションID |
| source_line_id | source_line_id | 原票明細ID |
| event_timestamp | event_timestamp | イベント発生日時 |
| load_timestamp | システム生成 | ETL実行時刻（UTC） |
| status_code | 'READY' or 'ERROR' | 処理ステータス |
| error_code | エラー時設定 | E_VALIDATION, E_MAPPING等 |
| error_message | エラー時設定 | エラー内容 |
| ledger_id | 固定値: 'GL001' | 総勘定元帳ID |
| legal_entity_id | 固定値: 'COMP001' | 法人ID |
| business_unit | 固定値: 'BU001' | 事業単位 |
| company_code | 固定値: 'COMP001' | 会社コード |
| accounting_date | invoice_date or order_date | 会計日付 |
| period_name | YYYY-MM形式 | 会計期間 |
| journal_category | 'Sales' | 仕訳カテゴリ |
| currency_code | currency_code | 通貨コード |
| exchange_rate_type | 'Corporate' | 為替レートタイプ |
| exchange_rate | exchange_rate or 1.0 | 為替レート |
| segment_account | ACCOUNT_MAPPING | 勘定科目（後述） |
| segment_department | DEPT_マッピング | 部門コード |
| segment_product | product_code | 商品コード |
| segment_project | NULL | プロジェクトコード（将来対応） |
| segment_interco | NULL | 内部取引（将来対応） |
| segment_custom1 | campaign_code | キャンペーンコード |
| segment_custom2 | salesperson_code | 営業担当者コード |
| customer_id | customer_code | 顧客コード |
| supplier_id | NULL | 仕入先コード（売上では使用しない） |
| invoice_id | invoice_id | 請求書ID |
| invoice_line_id | source_line_id | 請求書明細ID |
| quantity | quantity_shipped | 数量 |
| uom_code | unit_of_measure | 単位コード |
| tax_code | tax_code | 税コード |
| tax_rate | tax_rate | 税率 |
| tax_amount_entered | tax_amount | 税額（入力通貨） |
| revrec_rule_code | 'MONTHLY' | 収益認識ルール |
| revrec_start_date | invoice_date | 収益認識開始日 |
| description | 商品名＋顧客情報 | 摘要 |
| reference1 | order_id | 参照1（受注番号） |
| reference2 | shipment_id | 参照2（出荷番号） |
| reference3 | reference1 | 参照3 |
| reference4 | reference2 | 参照4 |
| reversal_flag | reversal_flag | 取消フラグ |
| reversed_interface_id | NULL | 取消元ID |
| created_by | 'ETL_SALES' | 作成者 |
| updated_by | 'ETL_SALES' | 更新者 |
| **entered_dr** | **0** | **借方金額（入力通貨）** ※売上は貸方のみ |
| **entered_cr** | **unit_price × quantity_shipped** | **貸方金額（入力通貨）** |
| **accounted_dr** | **0** | **借方金額（機能通貨）** |
| **accounted_cr** | **entered_cr × exchange_rate** | **貸方金額（機能通貨）** |

---

## 4. 変換ロジック仕様

### 4.1 勘定科目マッピング

#### 4.1.1 トランザクションタイプ別マッピング

| トランザクションタイプ | 借方勘定 | 貸方勘定 | 備考 |
|---------------------|---------|---------|------|
| INVOICE（請求） | 1100 売掛金 | 4100 売上高 | 請求書発行時 |
| SHIP（出荷） | 1100 売掛金 | 4100 売上高 | 出荷基準売上計上 |
| ORDER（受注） | - | - | 仕訳なし（受注段階） |
| CM（クレジットメモ） | 4100 売上高 | 1100 売掛金 | 返品・値引 |
| PMT（入金） | 1010 現金預金 | 1100 売掛金 | 入金消込 |

#### 4.1.2 本システムの対象範囲

**対象**: INVOICE, SHIP（売上計上）  
**対象外**: ORDER（受注のみ）、CM（返品）、PMT（入金）← 将来対応

### 4.2 税額計算ロジック

```python
# 税額計算
tax_amount = round(unit_price * quantity_shipped * tax_rate, 2)

# 総額（税込）
total_amount = (unit_price * quantity_shipped) + tax_amount
```

**税区分**:
- TAX_STANDARD: 標準税率 10%
- TAX_REDUCED: 軽減税率 8%
- TAX_EXEMPT: 非課税 0%

### 4.3 外貨換算ロジック

```python
# 機能通貨（JPY）への換算
if currency_code == 'JPY':
    accounted_amount = entered_amount
else:
    accounted_amount = entered_amount * exchange_rate
    accounted_amount = round(accounted_amount, 2)
```

**為替レートタイプ**: Corporate（社内公式レート）

### 4.4 エラーハンドリング

#### 4.4.1 必須項目チェック

以下の項目が欠落している場合、ERROR statusで出力：

- source_txn_id
- source_line_id
- txn_type
- customer_code
- product_code
- unit_price
- quantity_shipped (INVOICE, SHIP時)
- currency_code
- tax_code
- tax_rate

**エラーコード**: E_VALIDATION

#### 4.4.2 マッピングエラー

- 未登録の顧客コード
- 未登録の商品コード  
- 未登録の税コード

**エラーコード**: E_MAPPING

---

## 5. 実装仕様

### 5.1 モジュール

**ファイル**: `src/etl/sales_transformer.py`

**クラス**: `SalesTransformer`

**主要メソッド**:

```python
class SalesTransformer:
    def __init__(self, batch_id: str):
        # 初期化
        self.batch_id = batch_id
        self.load_timestamp = datetime.now(timezone.utc)
    
    def transform_record(self, source_record: dict) -> dict:
        # 1レコードを会計仕訳に変換
        # 
        # Args:
        #     source_record: 入力CSVの1行（辞書形式）
        # 
        # Returns:
        #     会計インターフェースレコード（辞書形式）
        # 
        # Raises:
        #     ValidationError: 必須項目不足
        #     MappingError: マッピングエラー
        pass
```

### 5.2 処理フロー

```
1. 入力CSVを読込
   └── test_data/sales/sales_txn_export.csv

2. 1行ずつ変換処理
   ├── 必須項目チェック
   ├── トランザクションタイプ判定
   ├── 勘定科目決定
   ├── 税額計算
   ├── 外貨換算
   └── 会計レコード生成

3. エラーハンドリング
   ├── ERROR statusで出力
   └── error_thresholdを超えたら処理中断

4. 出力CSVへ書込
   └── output/accounting_txn_interface_sales.csv（個別ファイル）

5. 統合処理（オーケストレーターが実行）
   └── output/accounting_txn_interface.csv（統合ファイル）
```

---

## 6. テストケース

### 6.1 正常系

| No | テストケース | 期待結果 |
|----|------------|---------|
| 1 | 通常の請求（INVOICE） | segment_account=4100, 貸方=unit_price×quantity |
| 2 | 出荷（SHIP） | segment_account=4100, 貸方=unit_price×quantity |
| 3 | 外貨取引（USD） | accounted_cr = entered_cr × exchange_rate |
| 4 | 税込取引（TAX_STANDARD 10%） | tax_amount = 金額 × 0.10 |

### 6.2 異常系

| No | テストケース | 期待結果 |
|----|------------|---------|
| 1 | customer_code が NULL | status_code=ERROR, error_code=E_VALIDATION |
| 2 | unit_price が 0 | status_code=ERROR, error_code=E_VALIDATION |
| 3 | 未登録の税コード | status_code=ERROR, error_code=E_MAPPING |
| 4 | quantity_shipped が NULL（INVOICE時） | status_code=ERROR, error_code=E_VALIDATION |

---

## 7. サンプルデータ

### 7.1 入力サンプル（sales_txn_export.csv）

```csv
export_id,source_txn_id,source_line_id,txn_type,event_timestamp,customer_code,product_code,quantity_shipped,unit_price,currency_code,tax_code,tax_rate,invoice_id,invoice_date
1,TXN000001,LINE000001,INVOICE,2025-10-26 12:00:00,CUST0001,PROD0001,10.000000,150000.00,JPY,TAX_STANDARD,0.100000,INV000001,2025-10-26
2,TXN000002,LINE000001,SHIP,2025-10-26 13:00:00,CUST0002,PROD0002,5.000000,200000.00,USD,TAX_STANDARD,0.100000,INV000002,2025-10-26
```

### 7.2 出力サンプル（accounting_txn_interface.csv）

```csv
batch_id,source_system,source_doc_type,source_doc_id,source_line_id,segment_account,customer_id,entered_dr,entered_cr,accounted_dr,accounted_cr
SALE_20251026_120000,SALE,INVOICE,TXN000001,LINE000001,4100,CUST0001,0,1500000.00,0,1500000.00
SALE_20251026_120000,SALE,SHIP,TXN000002,LINE000001,4100,CUST0002,0,1000000.00,0,150000000.00
```

（注: 上記は主要項目のみ抜粋）

---

## 付録

### A. 関連ドキュメント

- [基本設計書 - アーキテクチャ編](../basic_design/01_architecture_design.md)
- [基本設計書 - モジュール・データモデル編](../basic_design/02_module_data_design.md)
- [README](../../README.md)

### B. 参照コード

- `src/etl/sales_transformer.py`: 変換ロジック実装
- `src/local/standalone_sales_etl_job.py`: ローカル実行ジョブ
- `src/aws_glue/sales_etl_job.py`: AWS Glue実行ジョブ
