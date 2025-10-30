# 人事データETLジョブ（ローカル実行用・PySpark版）

import sys
import os
import argparse
from datetime import datetime
import logging

# プロジェクトルートをパスに追加
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../../..')))

from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, StringType
from src.etl.hr_transformer import HRTransformer

# ロギング設定
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def main():
    parser = argparse.ArgumentParser(description='HR ETL Job (Local PySpark)')
    parser.add_argument('--input-dir', type=str, default='test_data/hr',
                        help='Input directory path')
    parser.add_argument('--input-file', type=str, default='hr_employee_org_export.csv',
                        help='Input CSV filename (relative to input-dir)')
    parser.add_argument('--output-dir', type=str, default='output',
                        help='Output directory path')
    parser.add_argument('--output-file', type=str, default='accounting_txn_interface_hr.csv',
                        help='Output CSV filename (relative to output-dir)')
    parser.add_argument('--payroll-period', type=str, default=datetime.now().strftime('%Y-%m'),
                        help='Payroll period (YYYY-MM format)')
    parser.add_argument('--limit', type=int, default=None,
                        help='Limit number of records to process')
    parser.add_argument('--error-threshold', type=int, default=50,
                        help='Maximum number of errors before aborting')
    parser.add_argument('--master', type=str, default='local[*]',
                        help='Spark master URL (default: local[*] for all cores)')
    
    args = parser.parse_args()
    
    # Sparkセッションの初期化
    spark = SparkSession.builder \
        .appName("HR ETL Job (Local)") \
        .master(args.master) \
        .config("spark.driver.memory", "2g") \
        .config("spark.executor.memory", "2g") \
        .config("spark.sql.shuffle.partitions", "4") \
        .getOrCreate()
    
    # ログレベルをWARNに設定（Sparkの冗長なログを抑制）
    spark.sparkContext.setLogLevel("WARN")
    
    try:
        logger.info("=" * 60)
        logger.info("HR ETL Job Started (Local PySpark Execution)")
        logger.info("=" * 60)
        
        batch_id = f"HR_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        # パスを構築
        input_path = os.path.join(args.input_dir, args.input_file)
        output_path = os.path.join(args.output_dir, args.output_file)
        
        logger.info(f"Batch ID: {batch_id}")
        logger.info(f"Input File: {input_path}")
        logger.info(f"Output File: {output_path}")
        logger.info(f"Payroll Period: {args.payroll_period}")
        logger.info(f"Error Threshold: {args.error_threshold}")
        logger.info(f"Spark Master: {args.master}")
        logger.info("=" * 60)
        
        # CSVから人事データを読み込み（PySpark DataFrame）
        logger.info(f"Reading source data from {input_path}...")
        df = spark.read.csv(
            input_path,
            header=True,
            inferSchema=True,
            encoding='utf-8'
        )
        
        record_count = df.count()
        if record_count == 0:
            logger.warning("No source data found")
            spark.stop()
            return
        
        logger.info(f"Fetched {record_count} records from source")
        
        # データ変換（PySpark RDDで並列処理）
        logger.info("Transforming HR data to accounting entries...")
        
        # batch_id、payroll_period、project_rootをブロードキャスト
        batch_id_bc = spark.sparkContext.broadcast(batch_id)
        payroll_period_bc = spark.sparkContext.broadcast(args.payroll_period)
        project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../..'))
        project_root_bc = spark.sparkContext.broadcast(project_root)
        
        # DataFrameをRDDに変換して並列処理
        source_rdd = df.rdd.map(lambda row: row.asDict())
        
        # 変換処理を並列実行（各ワーカーでTransformerをインスタンス化）
        def transform_with_error_handling(record):
            try:
                # ワーカープロセスでsys.pathを設定
                import sys
                if project_root_bc.value not in sys.path:
                    sys.path.insert(0, project_root_bc.value)
                
                # 各ワーカープロセスでTransformerをインスタンス化
                from src.etl.hr_transformer import HRTransformer
                from datetime import datetime
                
                transformer = HRTransformer(batch_id_bc.value)
                
                # ダミーの給与データを生成（実際の運用では別CSVから読み込み）
                payroll_data = {
                    'payroll_period': payroll_period_bc.value,
                    'payment_date': datetime.now().strftime('%Y-%m-%d'),
                    'basic_salary': 300000,
                    'allowances': {
                        'HOUSING': 50000,
                        'TRANSPORTATION': 20000
                    },
                    'deductions': {
                        'TAX': 30000,
                        'INSURANCE': 15000
                    },
                    'currency_code': 'JPY'
                }
                
                transformed = transformer.transform_payroll_record(record, payroll_data)
                if transformed:
                    # 複数レコードを返す場合があるのでリスト化
                    result_list = transformed if isinstance(transformed, list) else [transformed]
                    return ('success', result_list)
                else:
                    return ('success', [])
            except Exception as e:
                return ('error', {'record': record, 'error': str(e)})
        
        result_rdd = source_rdd.map(transform_with_error_handling)
        
        # 成功とエラーを分離
        success_rdd = result_rdd.filter(lambda x: x[0] == 'success').flatMap(lambda x: x[1])
        error_rdd = result_rdd.filter(lambda x: x[0] == 'error')
        
        # 結果を収集
        accounting_records = success_rdd.collect()
        error_records = error_rdd.collect()
        error_count = len(error_records)
        
        logger.info(f"Transformation completed: {len(accounting_records)} accounting entries generated")
        logger.info(f"Errors: {error_count}")
        
        # エラー詳細をログ出力
        if error_records:
            logger.error("Error details:")
            for err in error_records[:5]:
                logger.error(f"  Record ID: {err[1].get('record', {}).get('export_id')}, Error: {err[1].get('error')}")
        
        # エラー閾値チェック
        if error_count >= args.error_threshold:
            logger.error(f"Error threshold ({args.error_threshold}) exceeded. Aborting.")
            raise Exception(f"Too many errors: {error_count}")
        
        # CSVへ書き込み（Python標準ライブラリ）
        if accounting_records:
            logger.info(f"Writing to {output_path}...")
            
            import csv
            os.makedirs(args.output_dir, exist_ok=True)
            
            # CSVファイルに書き込み
            with open(output_path, 'w', newline='', encoding='utf-8') as csvfile:
                if accounting_records:
                    # ヘッダーを最初のレコードのキーから取得
                    fieldnames = accounting_records[0].keys()
                    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                    
                    writer.writeheader()
                    writer.writerows(accounting_records)
            
            logger.info(f"Successfully wrote {len(accounting_records)} records")
        
        logger.info("=" * 60)
        logger.info("HR ETL Job Completed Successfully")
        logger.info("=" * 60)
        
    except Exception as e:
        logger.error("=" * 60)
        logger.error("HR ETL Job Failed")
        logger.error("=" * 60)
        logger.error(f"Error: {str(e)}")
        import traceback
        traceback.print_exc()
        logger.error("=" * 60)
        sys.exit(1)
    finally:
        # Sparkセッションを停止
        spark.stop()


if __name__ == "__main__":
    main()
