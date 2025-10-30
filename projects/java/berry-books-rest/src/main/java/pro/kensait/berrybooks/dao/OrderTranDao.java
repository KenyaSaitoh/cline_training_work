package pro.kensait.berrybooks.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.entity.OrderTran;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

// 注文テーブルへのアクセスを行うDAOクラス
@ApplicationScoped
public class OrderTranDao {
    private static final Logger logger = LoggerFactory.getLogger(
            OrderTranDao.class);

    @PersistenceContext(unitName = "bookstorePU")
    private EntityManager em;

    // DAOメソッド：顧客IDで注文履歴を取得
    public List<OrderTran> findByCustomerId(Integer customerId) {
        logger.info("[ OrderTranDao#findByCustomerId ]");
        
        TypedQuery<OrderTran> query = em.createQuery(
                "SELECT o FROM OrderTran o WHERE o.customerId = :customerId ORDER BY o.orderDate DESC", 
                OrderTran.class);
        query.setParameter("customerId", customerId);
        
        return query.getResultList();
    }

    // DAOメソッド：顧客IDで注文件数を取得
    public Long countOrdersByCustomerId(Integer customerId) {
        logger.info("[ OrderTranDao#countOrdersByCustomerId ]");
        
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(o) FROM OrderTran o WHERE o.customerId = :customerId", 
                Long.class);
        query.setParameter("customerId", customerId);
        
        return query.getSingleResult();
    }

    // DAOメソッド：顧客IDで購入冊数の合計を取得
    public Long sumBookCountByCustomerId(Integer customerId) {
        logger.info("[ OrderTranDao#sumBookCountByCustomerId ]");
        
        TypedQuery<Long> query = em.createQuery(
                "SELECT COALESCE(SUM(od.count), 0) FROM OrderDetail od WHERE od.orderTranId IN " +
                "(SELECT o.orderTranId FROM OrderTran o WHERE o.customerId = :customerId)", 
                Long.class);
        query.setParameter("customerId", customerId);
        
        return query.getSingleResult();
    }
}

