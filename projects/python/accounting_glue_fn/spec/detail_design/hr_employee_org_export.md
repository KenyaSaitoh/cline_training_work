# 人事・給与データエクスポート仕様書

## 1. 概要

人事システムから会計システムへの従業員・給与データのエクスポート仕様です。給与・手当・控除を会計仕訳に変換します。

### ドキュメント管理

| 項目 | 内容 |
|------|------|
| ドキュメント名 | 人事・給与データエクスポート詳細設計書 |
| バージョン | 3.0 |
| 作成日 | 2025-10-26 |
| 更新日 | 2025-10-29 |
| ステータス | 承認済み |

### 改訂履歴

| バージョン | 日付 | 内容 |
|-----------|------|------|
| 3.0 | 2025-10-29 | 必須項目バリデーション追加、Python Native/PySpark両対応 |
| 2.0 | 2025-10-26 | CSV入出力ベースへ更新 |
| 1.0 | 2025-10-25 | 初版作成 |

---

## 2. 入力データ

### 2.1 従業員マスタ: hr_employee_org_export.csv

#### 2.1.1 ファイル情報

| 項目 | 内容 |
|------|------|
| ファイル名 | hr_employee_org_export.csv |
| ファイルパス | test_data/hr/hr_employee_org_export.csv |
| 文字コード | UTF-8 |
| 形式 | CSV（ヘッダー行あり） |

#### 2.1.2 主要項目

| 項目名 | データ型 | 必須 | 説明 |
|--------|---------|------|------|
| export_id | INTEGER | ✔ | エクスポートID |
| employee_id | VARCHAR(40) | ✔ | 従業員ID（ジョインキー） |
| employee_number | VARCHAR(30) | ✔ | 社員番号 |
| last_name | VARCHAR(60) | ✔ | 姓 |
| first_name | VARCHAR(60) | ✔ | 名 |
| dept_code | VARCHAR(30) | ✔ | 部門コード |
| cost_center_code | VARCHAR(30) | - | コストセンタコード |
| payroll_group | VARCHAR(30) | - | 給与グループ |
| allocation_rule_code | VARCHAR(30) | - | 配賦ルールコード |
| tax_region_code | VARCHAR(20) | - | 課税地域コード |
| bank_account_no | VARCHAR(34) | - | 銀行口座番号 |

### 2.2 給与実績データ: hr_payroll_export.csv

#### 2.2.1 ファイル情報

| 項目 | 内容 |
|------|------|
| ファイル名 | hr_payroll_export.csv |
| ファイルパス | test_data/hr/hr_payroll_export.csv |
| 文字コード | UTF-8 |
| 形式 | CSV（ヘッダー行あり） |
| 件数 | 30件（従業員1人につき1レコード） |

#### 2.2.2 主要項目

| 項目名 | データ型 | 必須 | 説明 |
|--------|---------|------|------|
| payroll_id | VARCHAR(30) | ✔ | 給与ID（例: PAY202501_001） |
| employee_id | VARCHAR(40) | ✔ | 従業員ID（ジョインキー） |
| payroll_period | VARCHAR(7) | ✔ | 給与期間（YYYY-MM形式） |
| payment_date | DATE | ✔ | 支給日 |
| basic_salary | DECIMAL(15,2) | ✔ | 基本給 |
| allowance_housing | DECIMAL(15,2) | - | 住宅手当 |
| allowance_transportation | DECIMAL(15,2) | - | 交通費 |
| deduction_tax | DECIMAL(15,2) | - | 税金控除 |
| deduction_insurance | DECIMAL(15,2) | - | 保険料控除 |
| bonus | DECIMAL(15,2) | - | 賞与（0の場合はNULL扱い） |
| currency_code | VARCHAR(3) | ✔ | 通貨コード（例: JPY） |
| reversal_flag | BOOLEAN | ✔ | 逆仕訳フラグ（True/False） |
| update_timestamp | TIMESTAMP | ✔ | 更新タイムスタンプ |

#### 2.2.3 データ例

```csv
payroll_id,employee_id,payroll_period,payment_date,basic_salary,allowance_housing,allowance_transportation,deduction_tax,deduction_insurance,bonus,currency_code,reversal_flag,update_timestamp
PAY202501_001,EMP00001,2025-01,2025-01-25,450000,80000,30000,55000,25000,0,JPY,False,2025-01-20 10:00:00
PAY202501_002,EMP00002,2025-01,2025-01-25,350000,60000,25000,40000,20000,0,JPY,False,2025-01-20 10:00:00
```

---

## 3. 出力データ: accounting_txn_interface.csv

### 3.1 人事データ出力項目

| 出力項目 | 入力項目/計算式 | 説明 |
|---------|---------------|------|
| batch_id | システム生成 | 形式: HR_YYYYMMDD_HHMMSS |
| source_system | 固定値: 'HR' | ソースシステムコード |
| source_doc_type | 'PAYROLL' | 給与伝票 |
| source_doc_id | payroll_id | 給与ID |
| source_line_id | entry_seq | エントリ連番 |
| accounting_date | payroll_date | 給与支給日 |
| period_name | payroll_period | 給与期間（YYYY-MM） |
| journal_category | 'Payroll' | 仕訳カテゴリ |
| segment_account | ACCOUNT_MAPPING | 勘定科目（後述） |
| segment_department | department_code | 部門コード |
| created_by | 'ETL_HR' | 作成者 |
| updated_by | 'ETL_HR' | 更新者 |

---

## 4. 変換ロジック仕様

### 4.1 給与項目別勘定科目マッピング

#### 4.1.1 基本給

| 給与項目 | 借方勘定 | 貸方勘定 |
|---------|---------|---------|
| 基本給 | 6100 給与費用 | 2110 未払給与 |

**計算式**:
```python
entered_dr = basic_salary
entered_cr = 0

# 対になる仕訳
entered_dr = 0
entered_cr = basic_salary
```

#### 4.1.2 手当（allowances）

| 手当種別 | 借方勘定 | 貸方勘定 |
|---------|---------|---------|
| 交通費（TRANSPORT） | 6200 旅費交通費 | 2110 未払給与 |
| 住宅手当（HOUSING） | 6300 福利厚生費 | 2110 未払給与 |
| 家族手当（FAMILY） | 6100 給与費用 | 2110 未払給与 |

#### 4.1.3 控除（deductions）

| 控除種別 | 借方勘定 | 貸方勘定 |
|---------|---------|---------|
| 所得税（INCOME_TAX） | 2110 未払給与 | 2120 預り金-所得税 |
| 住民税（RESIDENT_TAX） | 2110 未払給与 | 2121 預り金-住民税 |
| 社会保険料（SOCIAL_INSURANCE） | 2110 未払給与 | 2130 預り金-社保 |
| 厚生年金（PENSION） | 2110 未払給与 | 2131 預り金-年金 |
| 健康保険（HEALTH） | 2110 未払給与 | 2132 預り金-健保 |
| 雇用保険（EMPLOYMENT） | 2110 未払給与 | 2133 預り金-雇保 |

### 4.2 複数エントリ生成（片側記録方式）

1従業員あたり、以下の**片側エントリ**を生成：

1. 基本給エントリ（借方：給与費用）
2. 手当エントリ × N（各手当ごと、借方：各種費用）
3. 控除エントリ × M（各控除ごと、貸方：預り金）

**合計**: 1 + N + M エントリ/従業員（すべて片側のみ記録）

**重要**: 対側仕訳（未払給与）はERP側の自動仕訳ルールで一括生成されます。

### 4.3 給与実績データの読み込みとジョイン処理

#### 4.3.1 データ読み込み

ETLジョブは以下の2つのCSVファイルを読み込みます：

1. **従業員マスタ** (`hr_employee_org_export.csv`)
   - 従業員の組織情報、部門コード、コストセンタなどを含む
   
2. **給与実績データ** (`hr_payroll_export.csv`)
   - 各従業員の給与額、手当、控除の実績データ

#### 4.3.2 ジョイン処理

**ジョインキー**: `employee_id`

**ジョイン方式**: INNER JOIN（給与実績がある従業員のみ処理）

**処理フロー**:

```python
# 1. 従業員マスタを読み込み、辞書化
employee_dict = {}
for employee in read_csv('hr_employee_org_export.csv'):
    employee_dict[employee['employee_id']] = employee

# 2. 給与実績を読み込み、従業員情報とジョイン
for payroll in read_csv('hr_payroll_export.csv'):
    employee_id = payroll['employee_id']
    if employee_id in employee_dict:
        hr_record = employee_dict[employee_id]
        
        # 給与データをDecimal型に変換
        payroll_data = {
            'payroll_id': payroll['payroll_id'],
            'payroll_period': payroll['payroll_period'],
            'payment_date': payroll['payment_date'],
            'basic_salary': Decimal(payroll['basic_salary']),
            'allowances': {
                'HOUSING': Decimal(payroll['allowance_housing']),
                'TRANSPORTATION': Decimal(payroll['allowance_transportation'])
            },
            'deductions': {
                'TAX': Decimal(payroll['deduction_tax']),
                'INSURANCE': Decimal(payroll['deduction_insurance'])
            },
            'bonus': Decimal(payroll['bonus']) if payroll['bonus'] else None,
            'currency_code': payroll['currency_code'],
            'reversal_flag': payroll['reversal_flag'] == 'True'
        }
        
        # 3. 会計仕訳に変換
        transform_payroll_record(hr_record, payroll_data)
```

#### 4.3.3 ジョイン結果

- **マッチしたレコード**: 会計仕訳に変換して出力
- **マッチしなかったレコード**: 
  - 給与実績のみ存在（従業員マスタなし）→ エラーログ出力、処理スキップ
  - 従業員マスタのみ存在（給与実績なし）→ 変換対象外（正常）

**注**: 実運用では給与計算システムと連携し、給与実績データは月次で更新されます

---

## 5. 実装仕様

### 5.1 モジュール

**ファイル**: `src/etl/hr_transformer.py`

**クラス**: `HRTransformer`

**主要メソッド**:

```python
class HRTransformer:
    def transform_payroll_record(self, 
                                hr_record: dict, 
                                payroll_data: dict) -> List[dict]:
        # 給与レコードを会計仕訳に変換（複数エントリ生成）
        # 
        # Args:
        #     hr_record: 従業員情報（CSV行）
        #     payroll_data: 給与情報
        # 
        # Returns:
        #     会計インターフェースレコードのリスト
        pass
```

### 5.2 処理フロー

```
1. 入力CSVを読込
   ├── test_data/hr/hr_employee_org_export.csv（従業員マスタ）
   └── test_data/hr/hr_payroll_export.csv（給与実績）

2. データジョイン
   └── employee_id をキーに INNER JOIN

3. 従業員ごとに処理
   ├── 基本給エントリ作成
   ├── 手当エントリ作成（複数）
   └── 控除エントリ作成（複数）

4. 出力CSVへ書込
   └── output/accounting_txn_interface_hr.csv（個別ファイル）

5. 統合処理
   └── output/accounting_txn_interface.csv（統合ファイル）
```

---

## 6. サンプルデータ

### 6.1 入力サンプル

#### 6.1.1 従業員マスタ（hr_employee_org_export.csv）

```csv
export_id,employee_id,employee_number,last_name,first_name,dept_code,cost_center_code
1,EMP00001,00001,山田,太郎,D001,CC001
2,EMP00002,00002,鈴木,花子,D002,CC002
```

#### 6.1.2 給与実績（hr_payroll_export.csv）

```csv
payroll_id,employee_id,payroll_period,payment_date,basic_salary,allowance_housing,allowance_transportation,deduction_tax,deduction_insurance,bonus,currency_code,reversal_flag
PAY202501_001,EMP00001,2025-01,2025-01-25,450000,80000,30000,55000,25000,0,JPY,False
PAY202501_002,EMP00002,2025-01,2025-01-25,350000,60000,25000,40000,20000,0,JPY,False
```

### 6.2 出力サンプル（accounting_txn_interface.csv）

```csv
batch_id,source_system,source_doc_type,source_doc_id,segment_account,segment_department,entered_dr,entered_cr
HR_20251026_120000,HR,PAYROLL,PAY_202510_EMP001,6100,DEPT_ACC,300000,0
HR_20251026_120000,HR,PAYROLL,PAY_202510_EMP001,2110,DEPT_ACC,0,300000
HR_20251026_120000,HR,PAYROLL,PAY_202510_EMP001,6200,DEPT_ACC,20000,0
HR_20251026_120000,HR,PAYROLL,PAY_202510_EMP001,2110,DEPT_ACC,0,20000
```

（注: 基本給と交通費の例、実際はさらに多数のエントリが生成される）

---

## 付録

### A. 関連ドキュメント

- [基本設計書 - アーキテクチャ編](../basic_design/01_architecture_design.md)
- [基本設計書 - モジュール・データモデル編](../basic_design/02_module_data_design.md)
- [README](../../README.md)

### B. 参照コード

- `src/etl/hr_transformer.py`: 変換ロジック実装
- `src/local/standalone_hr_etl_job.py`: ローカル実行ジョブ
- `src/aws_glue/hr_etl_job.py`: AWS Glue実行ジョブ
