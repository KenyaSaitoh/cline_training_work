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

## 2. 入力データ: hr_employee_org_export.csv

### 2.1 ファイル情報

| 項目 | 内容 |
|------|------|
| ファイル名 | hr_employee_org_export.csv |
| ファイルパス | test_data/hr/hr_employee_org_export.csv |
| 文字コード | UTF-8 |
| 形式 | CSV（ヘッダー行あり） |

### 2.2 主要項目

| 項目名 | データ型 | 必須 | 説明 |
|--------|---------|------|------|
| export_id | INTEGER | ✔ | エクスポートID |
| employee_id | VARCHAR(40) | ✔ | 従業員ID |
| employee_name | VARCHAR(200) | ✔ | 従業員名 |
| department_code | VARCHAR(30) | ✔ | 部門コード |
| department_name | VARCHAR(200) | - | 部門名 |
| job_grade | VARCHAR(30) | - | 職級 |
| employment_type | VARCHAR(20) | ✔ | 雇用形態（REGULAR/CONTRACT/PART_TIME） |
| hire_date | DATE | - | 入社日 |
| termination_date | DATE | - | 退職日 |
| email | VARCHAR(200) | - | メールアドレス |
| phone | VARCHAR(50) | - | 電話番号 |

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

### 4.3 給与データ生成ロジック

本システムでは、サンプル給与データを生成：

```python
def get_payroll_data(hr_record, payroll_period):
    # 給与データ生成（サンプル実装）
    
    # 雇用形態別の基本給
    if employment_type == 'REGULAR':
        basic_salary = 300000
    elif employment_type == 'CONTRACT':
        basic_salary = 250000
    elif employment_type == 'PART_TIME':
        basic_salary = 150000
    
    # 手当
    allowances = {
        'TRANSPORT': 20000,    # 交通費
        'HOUSING': 50000,       # 住宅手当
        'FAMILY': 15000         # 家族手当
    }
    
    # 控除
    deductions = {
        'INCOME_TAX': 25000,          # 所得税
        'RESIDENT_TAX': 18000,        # 住民税
        'SOCIAL_INSURANCE': 45000,    # 社会保険料
        'PENSION': 28000,             # 厚生年金
        'HEALTH': 15000,              # 健康保険
        'EMPLOYMENT': 2000            # 雇用保険
    }
```

**注**: 実運用では給与計算システムと連携

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
   └── test_data/hr/hr_employee_org_export.csv

2. 従業員ごとに処理
   ├── 給与データ生成
   ├── 基本給エントリ作成
   ├── 手当エントリ作成（複数）
   └── 控除エントリ作成（複数）

3. 出力CSVへ書込
   └── output/accounting_txn_interface_hr.csv（個別ファイル）

4. 統合処理
   └── output/accounting_txn_interface.csv（統合ファイル）
```

---

## 6. サンプルデータ

### 6.1 入力サンプル（hr_employee_org_export.csv）

```csv
export_id,employee_id,employee_name,department_code,employment_type
1,EMP001,山田太郎,DEPT_ACC,REGULAR
2,EMP002,鈴木花子,DEPT_DEV,CONTRACT
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
