# 在庫移動データエクスポート仕様書

## 1. 概要

在庫システムから会計システムへの在庫移動データのエクスポート仕様です。入庫・出庫・調整を会計仕訳に変換します。

### ドキュメント管理

| 項目 | 内容 |
|------|------|
| ドキュメント名 | 在庫移動データエクスポート詳細設計書 |
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

## 2. 入力データ: inv_movement_export.csv

### 2.1 ファイル情報

| 項目 | 内容 |
|------|------|
| ファイル名 | inv_movement_export.csv |
| ファイルパス | test_data/inventory/inv_movement_export.csv |
| 文字コード | UTF-8 |
| 形式 | CSV（ヘッダー行あり） |

### 2.2 主要項目

| 項目名 | データ型 | 必須 | 説明 |
|--------|---------|------|------|
| export_id | INTEGER | ✔ | エクスポートID |
| movement_id | VARCHAR(40) | ✔ | 移動ID |
| movement_type | VARCHAR(20) | ✔ | 移動タイプ（RCV/ISS/ADJ） |
| movement_timestamp | TIMESTAMP | ✔ | 移動日時 |
| product_code | VARCHAR(40) | ✔ | 商品コード |
| product_name | VARCHAR(200) | - | 商品名 |
| quantity | DECIMAL(19,6) | ✔ | 数量 |
| unit_of_measure | VARCHAR(10) | ✔ | 単位 |
| unit_cost | DECIMAL(19,2) | ✔ | 単位原価 |
| total_cost | DECIMAL(19,2) | - | 総原価 |
| warehouse_code | VARCHAR(30) | ✔ | 倉庫コード |
| location_code | VARCHAR(30) | - | ロケーションコード |
| lot_number | VARCHAR(40) | - | ロット番号 |
| serial_number | VARCHAR(40) | - | シリアル番号 |

---

## 3. 出力データ: accounting_txn_interface.csv

### 3.1 在庫データ出力項目

| 出力項目 | 入力項目/計算式 | 説明 |
|---------|---------------|------|
| batch_id | システム生成 | 形式: INV_YYYYMMDD_HHMMSS |
| source_system | 固定値: 'INV' | ソースシステムコード |
| source_doc_type | movement_type | RCV/ISS/ADJ |
| source_doc_id | movement_id | 移動ID |
| accounting_date | movement_timestamp | 移動日 |
| period_name | YYYY-MM形式 | 会計期間 |
| journal_category | 'Inventory' | 仕訳カテゴリ |
| segment_account | ACCOUNT_MAPPING | 勘定科目（後述） |
| segment_product | product_code | 商品コード |
| segment_department | warehouse_code | 倉庫コード |
| quantity | quantity | 数量 |
| uom_code | unit_of_measure | 単位コード |
| created_by | 'ETL_INV' | 作成者 |
| updated_by | 'ETL_INV' | 更新者 |

---

## 4. 変換ロジック仕様

### 4.1 移動タイプ別勘定科目マッピング

| 移動タイプ | コード | 借方勘定 | 貸方勘定 | 説明 |
|-----------|------|---------|---------|------|
| 入庫 | RCV | 1300 棚卸資産 | 2100 買掛金 | 仕入による入庫 |
| 出庫 | ISS | 5100 売上原価 | 1300 棚卸資産 | 販売による出庫 |
| 調整（増） | ADJ | 1300 棚卸資産 | 5200 棚卸差異 | 棚卸による増加 |
| 調整（減） | ADJ | 5200 棚卸差異 | 1300 棚卸資産 | 棚卸による減少 |

### 4.2 金額計算（片側記録方式）

```python
# 総原価計算
cost_amount = quantity * unit_cost
cost_amount = round(cost_amount, 2)

# 片側記録方式: すべて借方に記録
if movement_type == 'RCV':
    # 入庫: 借方=棚卸資産
    entered_dr = cost_amount
    entered_cr = 0
    segment_account = '1300'
elif movement_type == 'ISS':
    # 出庫: 借方=売上原価
    entered_dr = cost_amount
    entered_cr = 0
    segment_account = '5100'
elif movement_type == 'ADJ':
    if quantity > 0:
        # 在庫増加: 借方=棚卸資産
        entered_dr = cost_amount
        entered_cr = 0
        segment_account = '1300'
    else:
        # 在庫減少: 借方=棚卸差異
        entered_dr = abs(cost_amount)
        entered_cr = 0
        segment_account = '5200'

# 対側仕訳はERP側で自動生成される
```

### 4.3 原価評価方法

- **採用方法**: 移動平均法
- **単位原価**: 入庫時に記録された原価を使用
- **評価差異**: 月次で調整仕訳を計上（将来対応）

---

## 5. 実装仕様

### 5.1 モジュール

**ファイル**: `src/etl/inventory_transformer.py`

**クラス**: `InventoryTransformer`

**主要メソッド**:

```python
class InventoryTransformer:
    def transform_record(self, source_record: dict) -> dict:
        # 在庫移動レコードを会計仕訳に変換
        # 
        # Args:
        #     source_record: 入力CSVの1行
        # 
        # Returns:
        #     会計インターフェースレコード
        pass
```

### 5.2 処理フロー

```
1. 入力CSVを読込
   └── test_data/inventory/inv_movement_export.csv

2. 移動ごとに処理
   ├── 移動タイプ判定
   ├── 勘定科目決定
   ├── 原価計算
   └── 会計レコード生成

3. 出力CSVへ書込
   └── output/accounting_txn_interface_inventory.csv（個別ファイル）

4. 統合処理
   └── output/accounting_txn_interface.csv（統合ファイル）
```

---

## 6. テストケース

### 6.1 正常系

| No | テストケース | 期待結果 |
|----|------------|---------|
| 1 | 入庫（RCV） | 借方=1300（棚卸資産）、貸方=2100（買掛金） |
| 2 | 出庫（ISS） | 借方=5100（売上原価）、貸方=1300（棚卸資産） |
| 3 | 調整（増） | 借方=1300（棚卸資産）、貸方=5200（棚卸差異） |
| 4 | 調整（減） | 借方=5200（棚卸差異）、貸方=1300（棚卸資産） |

### 6.2 異常系

| No | テストケース | 期待結果 |
|----|------------|---------|
| 1 | product_code が NULL | status_code=ERROR, error_code=E_VALIDATION |
| 2 | quantity が 0 | status_code=ERROR, error_code=E_VALIDATION |
| 3 | unit_cost が NULL | status_code=ERROR, error_code=E_VALIDATION |

---

## 7. サンプルデータ

### 7.1 入力サンプル（inv_movement_export.csv）

```csv
export_id,movement_id,movement_type,movement_timestamp,product_code,quantity,unit_of_measure,unit_cost,warehouse_code
1,MOV000001,RCV,2025-10-26 10:00:00,PROD0001,100.000000,EA,5000.00,WH001
2,MOV000002,ISS,2025-10-26 14:00:00,PROD0001,10.000000,EA,5000.00,WH001
3,MOV000003,ADJ,2025-10-26 18:00:00,PROD0002,5.000000,EA,3000.00,WH001
```

### 7.2 出力サンプル（accounting_txn_interface.csv）

```csv
batch_id,source_system,source_doc_type,source_doc_id,segment_account,segment_product,quantity,entered_dr,entered_cr
INV_20251026_120000,INV,RCV,MOV000001,1300,PROD0001,100.000000,500000.00,0
INV_20251026_120000,INV,ISS,MOV000002,5100,PROD0001,10.000000,50000.00,0
INV_20251026_120000,INV,ADJ,MOV000003,1300,PROD0002,5.000000,15000.00,0
```

---

## 付録

### A. 関連ドキュメント

- [基本設計書 - アーキテクチャ編](../basic_design/01_architecture_design.md)
- [基本設計書 - モジュール・データモデル編](../basic_design/02_module_data_design.md)
- [README](../../README.md)

### B. 参照コード

- `src/etl/inventory_transformer.py`: 変換ロジック実装
- `src/local/standalone_inventory_etl_job.py`: ローカル実行ジョブ
- `src/aws_glue/inventory_etl_job.py`: AWS Glue実行ジョブ
