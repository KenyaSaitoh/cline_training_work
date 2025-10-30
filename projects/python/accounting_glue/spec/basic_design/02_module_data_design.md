# ERP会計統合ETLシステム 基本設計書

## 第2部: モジュール設計・データモデル設計

### ドキュメント管理

| 項目 | 内容 |
|------|------|
| ドキュメント名 | ERP会計統合ETLシステム 基本設計書 - モジュール・データモデル編 |
| バージョン | 3.0 |
| 作成日 | 2025-10-26 |
| 更新日 | 2025-10-29 |
| ステータス | 承認済み |

### 改訂履歴

| バージョン | 日付 | 内容 |
|-----------|------|------|
| 3.0 | 2025-10-29 | テスト環境追加、バリデーション強化、Python Native/PySpark両対応 |
| 2.0 | 2025-10-26 | CSV入出力ベースへ全面刷新、データベースモジュール削除 |
| 1.0 | 2025-10-25 | 初版作成 |

---

## 1. モジュール構成

### 1.1 全体モジュール構成

```
projects/python/accounting_glue/
├── src/
│   ├── local/                         # ローカル実行用ETLジョブ
│   │   ├── etl_orchestrator.py        # 統合オーケストレーター
│   │   ├── python_native/             # Python標準版
│   │   │   ├── standalone_sales_etl_job.py
│   │   │   ├── standalone_hr_etl_job.py
│   │   │   └── standalone_inventory_etl_job.py
│   │   └── pyspark/                   # PySpark版
│   │       ├── standalone_sales_etl_job.py
│   │       ├── standalone_hr_etl_job.py
│   │       └── standalone_inventory_etl_job.py
│   │
│   ├── aws_glue/                      # AWS Glue実行用ETLジョブ
│   │   ├── sales_etl_job.py
│   │   ├── hr_etl_job.py
│   │   └── inventory_etl_job.py
│   │
│   ├── common/                        # 環境共通モジュール
│   │   ├── config.py                  # 設定・定数定義
│   │   ├── csv_handler.py             # CSV入出力ハンドラー
│   │   └── utils.py                   # ユーティリティ
│   │
│   └── etl/                           # ETL変換モジュール（環境非依存）
│       ├── sales_transformer.py       # 売上データ変換
│       ├── hr_transformer.py          # 人事データ変換（必須項目バリデーション）
│       └── inventory_transformer.py   # 在庫データ変換
│
├── tests/                             # テストコード
│   ├── conftest.py                    # pytest共通設定
│   ├── unit/                          # 単体テスト
│   │   └── test_etl/
│   │       └── test_hr_transformer.py
│   └── integration/                   # 統合テスト
│       └── test_etl_output.py         # 出力検証（業務キー照合）
│
├── test_data/                         # テストデータ（入力CSV）
│   ├── sales/sales_txn_export.csv
│   ├── hr/hr_employee_org_export.csv
│   ├── inventory/inv_movement_export.csv
│   └── expected/                      # 期待値データ（テスト用）
│       ├── accounting_txn_interface_sales.csv
│       ├── accounting_txn_interface_hr.csv
│       └── accounting_txn_interface_inventory.csv
│
├── output/                            # 出力データ（ETL処理結果）
│   └── accounting_txn_interface.csv   # 統合ファイル（2,481件）
│
├── merge_etl_outputs.py               # 個別ファイル統合スクリプト
├── cleanup_output.py                  # 出力クリーンアップ
├── requirements.txt                   # Python依存（PySpark含む）
├── requirements-dev.txt               # 開発依存（pytest含む）
├── pytest.ini                         # pytestプロジェクト設定
└── README.md                          # システム説明書
```

---

## 2. 共通モジュール設計

### 2.1 config.py - 設定管理モジュール

#### 責務
- 会計勘定科目マッピングルールの定義
- 税コード、通貨コード等のマスタ定義
- エラーコード定義
- システムコード定義

#### 主要な定義

```python
# ソースシステム定義
SOURCE_SYSTEMS = {
    'SALE': {'code': 'SALE', 'name': '売上システム'},
    'HR': {'code': 'HR', 'name': '人事システム'},
    'INV': {'code': 'INV', 'name': '在庫システム'}
}

# 勘定科目マッピング
ACCOUNT_MAPPING = {
    # 売上関連
    'REVENUE': {
        'account': '4100',
        'name': '売上高'
    },
    'AR': {
        'account': '1100',
        'name': '売掛金'
    },
    # 人事関連
    'SALARY_EXPENSE': {
        'account': '6100',
        'name': '給与費用'
    },
    'PAYROLL_PAYABLE': {
        'account': '2110',
        'name': '未払給与'
    },
    # 在庫関連
    'INVENTORY': {
        'account': '1300',
        'name': '棚卸資産'
    },
    'COGS': {
        'account': '5100',
        'name': '売上原価'
    }
}

# 税コードマッピング
TAX_CODE_MAPPING = {
    'TAX_STANDARD': {'rate': 0.10, 'name': '標準税率'},
    'TAX_REDUCED': {'rate': 0.08, 'name': '軽減税率'},
    'TAX_EXEMPT': {'rate': 0.00, 'name': '非課税'}
}

# エラーコード
ERROR_CODES = {
    'E_VALIDATION': '検証エラー',
    'E_MAPPING': 'マッピングエラー',
    'E_CALCULATION': '計算エラー',
    'E_PROCESSING': '処理エラー'
}
```

### 2.2 csv_handler.py - CSV入出力モジュール

#### 責務
- CSVファイルの読み込み
- CSVファイルの書き込み
- 出力ディレクトリのクリーンアップ

#### 主要関数

```python
def read_csv(file_path: str) -> List[Dict[str, Any]]:
    # CSVファイルを読み込む
    # 
    # Args:
    #     file_path: CSVファイルパス
    # 
    # Returns:
    #     レコードのリスト（各レコードは辞書）
    # 
    # Raises:
    #     FileNotFoundError: ファイルが存在しない
    #     Exception: 読込エラー

def write_csv(file_path: str, records: List[Dict[str, Any]]):
    # CSVファイルを書き込む
    # 
    # Args:
    #     file_path: 出力CSVファイルパス
    #     records: レコードのリスト
    # 
    # Raises:
    #     Exception: 書込エラー

def cleanup_output_dir(output_dir: str) -> List[str]:
    # 出力ディレクトリをクリーンアップする
    # 
    # Args:
    #     output_dir: クリーンアップ対象ディレクトリ
    # 
    # Returns:
    #     削除したファイルパスのリスト
```

### 2.3 utils.py - ユーティリティモジュール

#### 責務
- 日付・時刻処理
- 数値計算（四捨五入、税額計算）
- バッチID生成
- データ検証

#### 主要クラス

```python
class ETLUtils:
    # ETL共通ユーティリティ
    
    @staticmethod
    def generate_batch_id(system_code: str) -> str:
        # バッチID生成
        # 形式: SALE_YYYYMMDD_HHMMSS
    
    @staticmethod
    def calculate_tax(amount: Decimal, tax_rate: Decimal) -> Decimal:
        # 税額計算
    
    @staticmethod
    def round_decimal(value: Decimal, precision: int) -> Decimal:
        # 四捨五入
    
    @staticmethod
    def validate_required_fields(record: dict, fields: list) -> bool:
        # 必須項目検証
```

---

## 3. ETL変換モジュール設計

### 3.1 Transformerの共通インターフェース

すべてのTransformerは以下の共通パターンに従う：

```python
class BaseTransformer:
    # Transformer基底クラス（概念的な定義）
    
    def __init__(self, batch_id: str):
        self.batch_id = batch_id
        self.load_timestamp = datetime.now(timezone.utc)
    
    def transform_record(self, source_record: dict) -> dict:
        # 1レコードを変換
        # 
        # Args:
        #     source_record: ソースレコード（CSV行データ）
        # 
        # Returns:
        #     会計インターフェースレコード
        raise NotImplementedError
    
    def _create_base_record(self, source_record: dict) -> dict:
        # 基本項目を設定
        pass
    
    def _create_error_record(self, source_record: dict, 
                            error_code: str, 
                            error_message: str) -> dict:
        # エラーレコードを生成
        pass
```

### 3.2 SalesTransformer - 売上データ変換

#### 責務
売上トランザクションを会計仕訳に変換する。

#### 変換ロジック

```python
class SalesTransformer:
    def transform_record(self, source_record: dict) -> dict:
        # 売上レコード変換
        # 
        # 変換ルール:
        # 1. トランザクションタイプに応じた勘定科目決定
        #    - INVOICE: 借方=売掛金(1100), 貸方=売上高(4100)
        #    - SHIP: 借方=売掛金(1100), 貸方=売上高(4100)
        #    - ORDER: 仕訳なし（受注段階）
        # 
        # 2. 税額計算
        #    - tax_amount = unit_price * quantity * tax_rate
        # 
        # 3. 外貨換算
        #    - JPY以外の場合、exchange_rateで換算
        # 
        # 4. セグメント情報設定
        #    - segment_department: 部門コード
        #    - segment_product: 商品コード
        #    - customer_id: 顧客コード
        pass
```

**出力項目マッピング**:

| 出力項目 | 入力項目/計算式 |
|---------|---------------|
| batch_id | システム生成（SALE_YYYYMMDD_HHMMSS） |
| source_system | 固定値: 'SALE' |
| source_doc_type | source_record['txn_type'] |
| source_doc_id | source_record['source_txn_id'] |
| source_line_id | source_record['source_line_id'] |
| event_timestamp | source_record['event_timestamp'] |
| accounting_date | source_record['invoice_date'] or source_record['order_date'] |
| segment_account | ACCOUNT_MAPPING['REVENUE']['account'] |
| customer_id | source_record['customer_code'] |
| entered_dr | 0 |
| entered_cr | unit_price * quantity_shipped |
| accounted_dr | 0 |
| accounted_cr | entered_cr * exchange_rate |

### 3.3 HRTransformer - 人事データ変換

#### 責務
従業員・給与データを会計仕訳に変換する。

#### 変換ロジック

```python
class HRTransformer:
    def transform_payroll_record(self, 
                                hr_record: dict, 
                                payroll_data: dict) -> List[dict]:
        # 給与レコード変換（複数エントリ生成）
        # 
        # バリデーション:
        # - employee_id必須（不在時エラーレコード返却）
        # - department_code必須（不在時エラーレコード返却）
        # 
        # 変換ルール:
        # 1. 基本給
        #    - 借方: 給与費用(6100)
        #    - 貸方: 未払給与(2110)
        # 
        # 2. 各種手当（allowances）
        #    - 交通費: 借方=旅費交通費(6200)
        #    - 住宅手当: 借方=福利厚生費(6300)
        # 
        # 3. 各種控除（deductions）
        #    - 所得税: 貸方=預り金-所得税(2120)
        #    - 社会保険料: 貸方=預り金-社保(2130)
        # 
        # 4. 部門配賦
        #    - segment_department: hr_record['department_code']
        # 
        # Returns:
        #     給与項目ごとの仕訳レコードリスト
        pass
```

**出力例**（1従業員あたり複数エントリ）:

```
エントリ1: 基本給
  借方: 給与費用(6100) 300,000円
  貸方: 未払給与(2110) 300,000円

エントリ2: 交通費
  借方: 旅費交通費(6200) 20,000円
  貸方: 未払給与(2110) 20,000円

エントリ3: 所得税控除
  借方: 未払給与(2110) 25,000円
  貸方: 預り金-所得税(2120) 25,000円
```

### 3.4 InventoryTransformer - 在庫データ変換

#### 責務
在庫移動トランザクションを会計仕訳に変換する。

#### 変換ロジック

```python
class InventoryTransformer:
    def transform_record(self, source_record: dict) -> dict:
        # 在庫移動レコード変換
        # 
        # 変換ルール:
        # 1. 移動タイプ別勘定科目決定
        #    - RCV (入庫): 借方=在庫(1300), 貸方=買掛金(2100)
        #    - ISS (出庫): 借方=売上原価(5100), 貸方=在庫(1300)
        #    - ADJ (調整): 借方/貸方=在庫(1300), 相手=棚卸差異(5200)
        # 
        # 2. 原価計算
        #    - cost_amount = unit_cost * quantity
        # 
        # 3. セグメント情報
        #    - segment_product: 商品コード
        #    - segment_department: 倉庫/拠点コード
        pass
```

**移動タイプ別マッピング**:

| 移動タイプ | 借方勘定 | 貸方勘定 |
|-----------|---------|---------|
| RCV（入庫） | 1300 棚卸資産 | 2100 買掛金 |
| ISS（出庫） | 5100 売上原価 | 1300 棚卸資産 |
| ADJ（調整）+ | 1300 棚卸資産 | 5200 棚卸差異 |
| ADJ（調整）- | 5200 棚卸差異 | 1300 棚卸資産 |

---

## 4. 実行モジュール設計

### 4.1 etl_orchestrator.py - 統合オーケストレーター

#### 責務
- 複数ETLジョブの並列/順次実行制御
- 個別出力ファイルの統合処理
- エラーハンドリングと結果レポート

#### 主要関数

```python
def run_parallel(systems: list, 
                input_base_dir: str, 
                output_dir: str) -> list:
    # 並列実行
    # 
    # - ThreadPoolExecutorで並列実行
    # - 各ジョブをsubprocessで起動
    # - 全ジョブ完了を待機
    pass

def run_sequential(systems: list, 
                  input_base_dir: str, 
                  output_dir: str) -> list:
    # 順次実行
    # 
    # - エラー発生時は後続ジョブをスキップ
    # - デバッグ用途
    pass

def merge_output_files(output_dir: str, 
                      output_file: str, 
                      target_systems: list, 
                      keep_individual_files: bool):
    # 個別ファイル統合
    # 
    # 1. 各システムの個別CSVを読込
    #    - accounting_txn_interface_sales.csv
    #    - accounting_txn_interface_hr.csv
    #    - accounting_txn_interface_inventory.csv
    # 
    # 2. 1つの統合CSVに結合
    #    - accounting_txn_interface.csv
    # 
    # 3. 個別ファイルを削除（オプション）
    pass
```

#### 実行フロー

```
1. パラメータ取得
   ↓
2. クリーンアップ（--cleanupオプション時）
   ↓
3. 並列/順次実行
   ├── standalone_sales_etl_job.py
   ├── standalone_hr_etl_job.py
   └── standalone_inventory_etl_job.py
   ↓
4. 個別ファイル統合
   merge_output_files()
   ↓
5. 結果サマリー出力
```

### 4.2 standalone_*_etl_job.py - 個別ETLジョブ

#### 共通構造

```python
def main():
    # 個別ETLジョブメイン処理
    
    # 1. パラメータ解析
    parser = argparse.ArgumentParser()
    parser.add_argument('--input-dir', default='test_data/sales')
    parser.add_argument('--input-file', default='sales_txn_export.csv')
    parser.add_argument('--output-dir', default='output')
    parser.add_argument('--output-file', default='accounting_txn_interface_sales.csv')
    parser.add_argument('--limit', type=int, default=None)
    parser.add_argument('--error-threshold', type=int, default=100)
    
    # 2. CSV読込
    input_path = os.path.join(args.input_dir, args.input_file)
    source_data = read_csv(input_path)
    
    # 3. 変換処理
    transformer = SalesTransformer(batch_id)
    accounting_records = []
    error_count = 0
    
    for source_record in source_data:
        try:
            target_record = transformer.transform_record(source_record)
            
            # datetime → 文字列変換
            for key, value in target_record.items():
                if isinstance(value, datetime):
                    target_record[key] = value.strftime('%Y-%m-%d %H:%M:%S')
            
            accounting_records.append(target_record)
        except Exception as e:
            error_count += 1
            if error_count > args.error_threshold:
                raise
    
    # 4. CSV出力
    output_path = os.path.join(args.output_dir, args.output_file)
    write_csv(output_path, accounting_records)
```

---

## 5. データモデル設計

### 5.1 入力データモデル（CSV）

#### test_data/sales/sales_txn_export.csv

**主要カラム**:

| カラム名 | データ型 | 説明 |
|---------|---------|------|
| export_id | INTEGER | エクスポートID |
| source_txn_id | VARCHAR | 原票トランザクションID |
| source_line_id | VARCHAR | 原票明細ID |
| txn_type | VARCHAR | トランザクションタイプ（ORDER/SHIP/INVOICE） |
| event_timestamp | TIMESTAMP | イベント発生日時 |
| customer_code | VARCHAR | 顧客コード |
| product_code | VARCHAR | 商品コード |
| quantity_shipped | DECIMAL | 出荷数量 |
| unit_price | DECIMAL | 単価 |
| currency_code | VARCHAR | 通貨コード |
| tax_code | VARCHAR | 税コード |
| tax_rate | DECIMAL | 税率 |

#### test_data/hr/hr_employee_org_export.csv

**主要カラム**:

| カラム名 | データ型 | 説明 |
|---------|---------|------|
| export_id | INTEGER | エクスポートID |
| employee_id | VARCHAR | 従業員ID |
| employee_name | VARCHAR | 従業員名 |
| department_code | VARCHAR | 部門コード |
| job_grade | VARCHAR | 職級 |
| employment_type | VARCHAR | 雇用形態 |

#### test_data/inventory/inv_movement_export.csv

**主要カラム**:

| カラム名 | データ型 | 説明 |
|---------|---------|------|
| export_id | INTEGER | エクスポートID |
| movement_id | VARCHAR | 移動ID |
| movement_type | VARCHAR | 移動タイプ（RCV/ISS/ADJ） |
| product_code | VARCHAR | 商品コード |
| quantity | DECIMAL | 数量 |
| unit_cost | DECIMAL | 単位原価 |
| warehouse_code | VARCHAR | 倉庫コード |

### 5.2 出力データモデル（CSV）

#### output/accounting_txn_interface.csv（統合ファイル）

**全カラム定義** (52カラム):

| カラム名 | データ型 | 必須 | 説明 |
|---------|---------|------|------|
| batch_id | VARCHAR(50) | ✔ | バッチID |
| source_system | VARCHAR(10) | ✔ | ソースシステムコード（SALE/HR/INV） |
| source_doc_type | VARCHAR(20) | ✔ | 原票伝票タイプ |
| source_doc_id | VARCHAR(40) | ✔ | 原票伝票ID |
| source_line_id | VARCHAR(40) | ✔ | 原票明細ID |
| event_timestamp | TIMESTAMP | ✔ | イベント発生日時 |
| load_timestamp | TIMESTAMP | ✔ | ロードタイムスタンプ |
| status_code | VARCHAR(20) | ✔ | ステータスコード（READY/ERROR） |
| error_code | VARCHAR(20) | - | エラーコード |
| error_message | VARCHAR(500) | - | エラーメッセージ |
| ledger_id | VARCHAR(30) | ✔ | 元帳ID |
| legal_entity_id | VARCHAR(30) | ✔ | 法人ID |
| business_unit | VARCHAR(30) | - | 事業単位 |
| company_code | VARCHAR(30) | ✔ | 会社コード |
| accounting_date | DATE | ✔ | 会計日付 |
| period_name | VARCHAR(20) | ✔ | 会計期間（YYYY-MM） |
| journal_category | VARCHAR(40) | ✔ | 仕訳カテゴリ |
| currency_code | VARCHAR(3) | ✔ | 通貨コード（JPY/USD/EUR） |
| exchange_rate_type | VARCHAR(20) | - | 為替レートタイプ |
| exchange_rate | DECIMAL(19,6) | ✔ | 為替レート |
| segment_account | VARCHAR(30) | ✔ | セグメント-勘定科目 |
| segment_department | VARCHAR(30) | - | セグメント-部門 |
| segment_product | VARCHAR(30) | - | セグメント-商品 |
| segment_project | VARCHAR(30) | - | セグメント-プロジェクト |
| segment_interco | VARCHAR(30) | - | セグメント-内部取引 |
| segment_custom1 | VARCHAR(30) | - | セグメント-カスタム1 |
| segment_custom2 | VARCHAR(30) | - | セグメント-カスタム2 |
| customer_id | VARCHAR(40) | - | 顧客ID |
| supplier_id | VARCHAR(40) | - | 仕入先ID |
| bank_account_id | VARCHAR(40) | - | 銀行口座ID |
| invoice_id | VARCHAR(40) | - | 請求書ID |
| invoice_line_id | VARCHAR(40) | - | 請求書明細ID |
| po_id | VARCHAR(40) | - | 発注ID |
| receipt_id | VARCHAR(40) | - | 入庫ID |
| asset_id | VARCHAR(40) | - | 資産ID |
| project_id | VARCHAR(40) | - | プロジェクトID |
| quantity | DECIMAL(19,6) | - | 数量 |
| uom_code | VARCHAR(10) | - | 単位コード |
| tax_code | VARCHAR(20) | - | 税コード |
| tax_rate | DECIMAL(7,6) | - | 税率 |
| tax_amount_entered | DECIMAL(19,2) | - | 税額（入力通貨） |
| revrec_rule_code | VARCHAR(30) | - | 収益認識ルールコード |
| revrec_start_date | DATE | - | 収益認識開始日 |
| revrec_end_date | DATE | - | 収益認識終了日 |
| description | VARCHAR(500) | - | 摘要 |
| reference1 | VARCHAR(100) | - | 参照1 |
| reference2 | VARCHAR(100) | - | 参照2 |
| reference3 | VARCHAR(100) | - | 参照3 |
| reference4 | VARCHAR(100) | - | 参照4 |
| reference5 | VARCHAR(100) | - | 参照5 |
| reversal_flag | BOOLEAN | ✔ | 取消フラグ |
| reversed_interface_id | VARCHAR(40) | - | 取消元インターフェースID |
| created_by | VARCHAR(50) | ✔ | 作成者 |
| updated_by | VARCHAR(50) | ✔ | 更新者 |
| **entered_dr** | **DECIMAL(19,2)** | ✔ | **借方金額（入力通貨）** |
| **entered_cr** | **DECIMAL(19,2)** | ✔ | **貸方金額（入力通貨）** |
| **accounted_dr** | **DECIMAL(19,2)** | ✔ | **借方金額（機能通貨）** |
| **accounted_cr** | **DECIMAL(19,2)** | ✔ | **貸方金額（機能通貨）** |

---

## 6. エラーハンドリング設計

### 6.1 エラー分類

| エラーコード | 説明 | 処理 |
|------------|------|------|
| E_VALIDATION | 入力検証エラー | ERROR statusで出力 |
| E_MAPPING | マッピングエラー | ERROR statusで出力 |
| E_CALCULATION | 計算エラー | ERROR statusで出力 |
| E_PROCESSING | 処理エラー | ログ記録、処理継続 |

### 6.2 エラーレコード出力

エラー発生時も、以下の最小限情報でCSV出力：

```csv
batch_id,source_system,source_doc_id,status_code,error_code,error_message,...
SALE_20251026_120000,SALE,TXN001,ERROR,E_VALIDATION,顧客コードが不正です,...
```

---

## 7. パフォーマンス設計

### 7.1 処理性能目標

| 項目 | 目標値 | 実績 |
|------|--------|------|
| 売上ETL（1000件） | 1秒以内 | Python Native: 0.8秒<br>PySpark: 2.5秒 |
| 人事ETL（150件） | 1秒以内 | Python Native: 0.3秒<br>PySpark: 2.2秒 |
| 在庫ETL（1331件） | 1秒以内 | Python Native: 0.9秒<br>PySpark: 2.8秒 |
| 統合処理 | 1秒以内 | 0.2秒 |
| **合計** | **3秒以内** | Python Native: **1.2秒**<br>PySpark: **7.5秒** |

### 7.2 最適化手法

**Python Native版:**
1. **並列実行**: ThreadPoolExecutorで3ジョブ同時実行
2. **バッチ処理**: メモリ上で一括変換
3. **シンプル処理**: CSV直接処理

**PySpark版:**
1. **ローカルモード**: local[*]（全CPUコア利用）
2. **RDD変換**: map()による並列変換
3. **broadcast変数**: プロジェクトパス、batch_idの共有

---

## 付録

### A. 関連ドキュメント

- [基本設計書 - アーキテクチャ編](./01_architecture_design.md)
- [詳細設計書 - 売上ETL](../detail_design/sales_txn_export.md)
- [詳細設計書 - 人事ETL](../detail_design/hr_employee_org_export.md)
- [詳細設計書 - 在庫ETL](../detail_design/inv_movement_export.md)
- [README](../../README.md)

### B. サンプルデータ

**入力例** (sales_txn_export.csv):
```csv
export_id,source_txn_id,txn_type,customer_code,product_code,quantity_shipped,unit_price,currency_code,tax_code,tax_rate
1,TXN001,INVOICE,CUST001,PROD001,10,1000,JPY,TAX_STANDARD,0.10
```

**出力例** (accounting_txn_interface.csv):
```csv
batch_id,source_system,source_doc_id,segment_account,entered_dr,entered_cr,accounted_dr,accounted_cr
SALE_20251026_120000,SALE,TXN001,4100,0,10000,0,10000
```
