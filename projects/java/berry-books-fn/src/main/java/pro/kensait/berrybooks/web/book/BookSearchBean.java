package pro.kensait.berrybooks.web.book;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.entity.Book;
import pro.kensait.berrybooks.service.book.BookService;
import pro.kensait.berrybooks.service.category.CategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

// 書籍検索画面のバッキングBean
@Named
@SessionScoped
public class BookSearchBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(
            BookSearchBean.class);

    @Inject
    private BookService bookService;

    @Inject
    private CategoryService categoryService;

    // 検索条件
    private Integer categoryId;
    private String keyword;

    // 検索結果
    private List<Book> bookList;

    // カテゴリマップ（セレクトボックス用）
    private Map<String, Integer> categoryMap;

    @PostConstruct
    public void init() {
        logger.info("[ BookSearchBean#init ]");
        
        // カテゴリマップを初期化
        categoryMap = new HashMap<>();
        categoryMap.put("", null);
        categoryMap.putAll(categoryService.getCategoryMap());
        
        // bookListは初期化時は全書籍を取得
        if (bookList == null || bookList.isEmpty()) {
            bookList = bookService.getBooksAll();
        }
    }

    // アクション：書籍を検索する（静的クエリ）
    public String search() {
        logger.info("[ BookSearchBean#search ] categoryId=" + categoryId + ", keyword=" + keyword);

        // 検索条件に基づいて書籍を検索
        if (categoryId != null && categoryId != 0) {
            if (keyword != null && !keyword.isEmpty()) {
                bookList = bookService.searchBook(categoryId, keyword);
            } else {
                bookList = bookService.searchBook(categoryId);
            }
        } else {
            if (keyword != null && !keyword.isEmpty()) {
                bookList = bookService.searchBook(keyword);
            } else {
                bookList = bookService.getBooksAll();
            }
        }

        // 検索結果を bookSelect ページに表示
        return "bookSelect?faces-redirect=true";
    }

    // アクション：書籍を検索する（動的クエリ）
    public String search2() {
        logger.info("[ BookSearchBean#search2 ] categoryId=" + categoryId + ", keyword=" + keyword);
        
        // 動的クエリで書籍を検索
        bookList = bookService.searchBookWithCriteria(categoryId, keyword);
        
        // 検索結果を bookSelect ページに表示
        return "bookSelect?faces-redirect=true";
    }

    // アクション：全書籍を読み込む（bookSelectページ用）
    public void loadAllBooks() {
        logger.info("[ BookSearchBean#loadAllBooks ]");
        bookList = bookService.getBooksAll();
    }

    // アクション：書籍リストを最新の状態に更新する（在庫数を含む）
    public void refreshBookList() {
        logger.info("[ BookSearchBean#refreshBookList ]");
        
        // 既存の検索条件を使用して書籍リストを再取得
        if (bookList == null || bookList.isEmpty()) {
            // 初回表示時は全書籍を取得
            bookList = bookService.getBooksAll();
        } else {
            // 既に検索が実行されている場合は、同じ条件で再検索
            // カテゴリとキーワードの両方が設定されている場合
            if (categoryId != null && categoryId != 0) {
                if (keyword != null && !keyword.isEmpty()) {
                    bookList = bookService.searchBook(categoryId, keyword);
                } else {
                    bookList = bookService.searchBook(categoryId);
                }
            } else {
                // キーワードのみ設定されている場合
                if (keyword != null && !keyword.isEmpty()) {
                    bookList = bookService.searchBook(keyword);
                } else {
                    // 検索条件がない場合は全書籍を取得
                    bookList = bookService.getBooksAll();
                }
            }
        }
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

    public List<Book> getBookList() {
        return bookList;
    }

    public Map<String, Integer> getCategoryMap() {
        return categoryMap;
    }
}

