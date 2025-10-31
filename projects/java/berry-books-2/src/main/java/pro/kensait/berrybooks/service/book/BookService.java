package pro.kensait.berrybooks.service.book;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.common.MessageUtil;
import pro.kensait.berrybooks.entity.Book;
import pro.kensait.berrybooks.entity.Stock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

// 書籍検索を行うサービスクラス
@ApplicationScoped
@Transactional
public class BookService {
    private static final Logger logger = LoggerFactory.getLogger(
            BookService.class);

    @PersistenceContext(unitName = "bookstorePU")
    private EntityManager em;

    // サービスメソッド：書籍検索（主キー検索）
    public Book getBook(Integer bookId) {
        logger.info("[ BookService#getBook ]");
        
        // DAOロジックを直接実装
        logger.info("[ BookDao#findById ]");
        Book book = em.find(Book.class, bookId);
        
        if (book == null) {
            throw new RuntimeException(MessageUtil.get("error.book.not-found") + bookId);
        }
        return book;
    }

    // サービスメソッド：在庫情報取得（主キー検索）
    public Stock getStock(Integer bookId) {
        logger.info("[ BookService#getStock ]");
        
        // DAOロジックを直接実装
        logger.info("[ StockDao#findById ]");
        Stock stock = em.find(Stock.class, bookId);
        
        if (stock == null) {
            throw new RuntimeException(MessageUtil.get("error.stock.not-found") + bookId);
        }
        return stock;
    }

    // サービスメソッド：書籍検索（全件検索）
    public List<Book> getBooksAll() {
        logger.info("[ BookService#getBooksAll ]");
        
        // DAOロジックを直接実装
        logger.info("[ BookDao#findAll ]");
        TypedQuery<Book> query = em.createQuery(
                "SELECT b FROM Book b", Book.class);
        List<Book> books = query.getResultList();
        
        // 各エンティティをデータベースから再読み込みして最新の在庫データを取得
        for (Book book : books) {
            em.refresh(book);
        }
        
        return books;
    }

    // サービスメソッド：書籍検索（カテゴリIDとキーワードによる条件検索）
    public List<Book> searchBook(Integer categoryId, String keyword) {
        logger.info("[ BookService#searchBook(categoryId, keyword) ]");
        
        // DAOロジックを直接実装
        logger.info("[ BookDao#query ]");
        TypedQuery<Book> query = em.createQuery(
                "SELECT b FROM Book b WHERE b.category.categoryId = :categoryId " +
                "AND b.bookName like :keyword", 
                Book.class);
        query.setParameter("categoryId", categoryId);
        query.setParameter("keyword", toLikeWord(keyword));
        
        List<Book> books = query.getResultList();
        
        // 各エンティティをデータベースから再読み込みして最新の在庫データを取得
        for (Book book : books) {
            em.refresh(book);
        }
        
        return books;
    }

    // サービスメソッド：書籍検索（カテゴリIDによる条件検索）
    public List<Book> searchBook(Integer categoryId) {
        logger.info("[ BookService#searchBook(categoryId) ]");
        
        // DAOロジックを直接実装
        logger.info("[ BookDao#queryByCategory ]");
        TypedQuery<Book> query = em.createQuery(
                "SELECT b FROM Book b WHERE b.category.categoryId = :categoryId", 
                Book.class);
        query.setParameter("categoryId", categoryId);
        
        List<Book> books = query.getResultList();
        
        // 各エンティティをデータベースから再読み込みして最新の在庫データを取得
        for (Book book : books) {
            em.refresh(book);
        }
        
        return books;
    }

    // サービスメソッド：書籍検索（キーワードによる条件検索）
    public List<Book> searchBook(String keyword) {
        logger.info("[ BookService#searchBook(keyword) ]");
        
        // DAOロジックを直接実装
        logger.info("[ BookDao#queryByKeyword ]");
        TypedQuery<Book> query = em.createQuery(
                "SELECT b FROM Book b WHERE b.bookName like :keyword", 
                Book.class);
        query.setParameter("keyword", toLikeWord(keyword));
        
        List<Book> books = query.getResultList();
        
        // 各エンティティをデータベースから再読み込みして最新の在庫データを取得
        for (Book book : books) {
            em.refresh(book);
        }
        
        return books;
    }

    // サービスメソッド：書籍検索（動的クエリの構築）
    public List<Book> searchBookWithCriteria(Integer categoryId, String keyword) {
        logger.info("[ BookService#searchBookWithCriteria ]");
        
        String likeKeyword = (keyword != null && !keyword.isEmpty()) 
                ? toLikeWord(keyword) : null;
        
        // DAOロジックを直接実装（Criteria API）
        logger.info("[ BookDao#searchWithCriteria ]");
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> cq = cb.createQuery(Book.class);
        Root<Book> book = cq.from(Book.class);

        // 動的に条件を構築
        List<Predicate> predicates = new ArrayList<>();
        
        if (categoryId != null) {
            predicates.add(cb.equal(
                    book.get("category").get("categoryId"), categoryId));
        }
        
        if (likeKeyword != null && !likeKeyword.isEmpty()) {
            predicates.add(cb.like(
                    book.get("bookName"), likeKeyword));
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        TypedQuery<Book> query = em.createQuery(cq);
        List<Book> books = query.getResultList();
        
        // 各エンティティをデータベースから再読み込みして最新の在庫データを取得
        for (Book book2 : books) {
            em.refresh(book2);
        }
        
        return books;
    }

    private String toLikeWord(String keyword) {
        return "%" + keyword + "%";
    }
}


