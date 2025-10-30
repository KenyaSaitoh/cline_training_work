package pro.kensait.berrybooks.common;

// バリデーションエラーメッセージの定数クラス
public final class ErrorMessage {
    
    // インスタンス化を防止
    private ErrorMessage() {
    }
    
    // ===== 共通メッセージ =====
    public static final String EMAIL_REQUIRED = "メールアドレスを入力してください";
    public static final String EMAIL_INVALID = "有効なメールアドレスを入力してください";
    public static final String PASSWORD_REQUIRED = "パスワードを入力してください";
    public static final String GENERAL_ERROR = "エラーが発生しました";
    
    // ===== 顧客登録関連 =====
    public static final String CUSTOMER_NAME_REQUIRED = "顧客名を入力してください";
    public static final String CUSTOMER_NAME_MAX_LENGTH = "顧客名は50文字以内で入力してください";
    public static final String EMAIL_MAX_LENGTH = "メールアドレスは100文字以内で入力してください";
    public static final String PASSWORD_LENGTH = "パスワードは8文字以上20文字以内で入力してください";
    public static final String BIRTHDAY_FORMAT = "生年月日はyyyy-MM-dd形式で入力してください（例：1990-01-15）";
    public static final String BIRTHDAY_PARSE_ERROR = "生年月日の形式が正しくありません（例：1990-01-15）";
    public static final String ADDRESS_MAX_LENGTH = "住所は200文字以内で入力してください";
    public static final String ADDRESS_INVALID_PREFECTURE = "住所は正しい都道府県名で始まる必要があります";
    public static final String REGISTRATION_ERROR = "登録中にエラーが発生しました";
    public static final String EMAIL_ALREADY_EXISTS = "このメールアドレスは既に登録されています";
    
    // ===== ログイン関連 =====
    public static final String LOGIN_FAILED = "ログインに失敗しました";
    public static final String LOGIN_INVALID_CREDENTIALS = "メールアドレスまたはパスワードが正しくありません";
    
    // ===== カート・注文関連 =====
    public static final String DELIVERY_ADDRESS_REQUIRED = "配送先住所を入力してください";
    public static final String DELIVERY_ADDRESS_MAX_LENGTH = "配送先住所は200文字以内で入力してください";
    public static final String DELIVERY_ADDRESS_INVALID_PREFECTURE = "配送先住所は正しい都道府県名で始まる必要があります";
    public static final String SETTLEMENT_TYPE_REQUIRED = "決済方法を選択してください";
    public static final String OUT_OF_STOCK_MESSAGE = "在庫不足";
    public static final String OUT_OF_STOCK = "在庫不足: ";
    public static final String OPTIMISTIC_LOCK_ERROR = "他のユーザーが同時に注文しました。もう一度お試しください";
    public static final String ORDER_PROCESSING_ERROR = "注文処理中にエラーが発生しました: ";
    
    // ===== データ検索エラー =====
    public static final String BOOK_NOT_FOUND = "Book not found: ";
    public static final String ORDER_TRAN_NOT_FOUND = "OrderTran not found for ID: ";
    public static final String ORDER_DETAIL_NOT_FOUND = "OrderDetail not found for PK: ";
}

