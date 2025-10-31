package pro.kensait.berrybooks.web.book;

// 書籍検索パラメータを保持するクラス
public class SearchParam {
    private Integer categoryId;
    private String keyword;

    public SearchParam() {
    }

    public SearchParam(Integer categoryId, String keyword) {
        this.categoryId = categoryId;
        this.keyword = keyword;
    }

    // アクセサメソッド
    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}

