package pro.kensait.berrybooks.service.category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.entity.Category;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

// カテゴリを取得するサービスクラス
@ApplicationScoped
public class CategoryService {
    private static final Logger logger = LoggerFactory.getLogger(
            CategoryService.class);

    @PersistenceContext(unitName = "bookstorePU")
    private EntityManager em;

    // サービスメソッド：カテゴリの取得（全件検索）
    public List<Category> getCategoriesAll() {
        logger.info("[ CategoryService#getCategoriesAll ]");
        
        // DAOロジックを直接実装
        logger.info("[ CategoryDao#findAll ]");
        TypedQuery<Category> query = em.createQuery(
                "SELECT c FROM Category c", Category.class);
        
        return query.getResultList();
    }

    // サービスメソッド：カテゴリマップの取得
    public Map<String, Integer> getCategoryMap() {
        logger.info("[ CategoryService#getCategoryMap ]");
        
        Map<String, Integer> categoryMap = new HashMap<>();
        
        // DAOロジックを直接実装
        logger.info("[ CategoryDao#findAll ]");
        TypedQuery<Category> query = em.createQuery(
                "SELECT c FROM Category c", Category.class);
        List<Category> categories = query.getResultList();
        
        for (Category category : categories) {
            categoryMap.put(category.getCategoryName(), category.getCategoryId());
        }
        
        return categoryMap;
    }
}


