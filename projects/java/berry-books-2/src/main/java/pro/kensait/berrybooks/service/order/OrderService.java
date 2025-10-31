package pro.kensait.berrybooks.service.order;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.common.MessageUtil;
import pro.kensait.berrybooks.entity.Book;
import pro.kensait.berrybooks.entity.OrderDetail;
import pro.kensait.berrybooks.entity.OrderDetailPK;
import pro.kensait.berrybooks.entity.OrderTran;
import pro.kensait.berrybooks.entity.Stock;
import pro.kensait.berrybooks.web.cart.CartItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

// 注文処理と注文履歴取得を行うサービスクラス
@ApplicationScoped
@Transactional
public class OrderService implements OrderServiceIF {
    private static final Logger logger = LoggerFactory.getLogger(
            OrderService.class);

    @PersistenceContext(unitName = "bookstorePU")
    private EntityManager em;

    // サービスメソッド：注文エンティティのリストを取得する（方式1）
    @Override
    public List<OrderTran> getOrderHistory(Integer customerId) {
        logger.info("[ OrderService#findOrderHistory ]");

        // 顧客IDから注文エンティティのリストを取得し、返す
        // DAOロジックを直接実装
        logger.info("[ OrderTranDao#findByCustomerId ]");
        TypedQuery<OrderTran> query = em.createQuery(
                "SELECT ot FROM OrderTran ot INNER JOIN ot.orderDetails od " +
                "WHERE ot.customerId = :customerId " +
                "GROUP BY ot.orderTranId " +
                "ORDER BY ot.orderDate DESC",
                OrderTran.class);
        query.setParameter("customerId", customerId);
        
        List<OrderTran> orderTranList = query.getResultList();
        return orderTranList;
    }

    // サービスメソッド：注文履歴のリストを取得する（方式2・詳細版）
    @Override
    public List<OrderHistoryTO> getOrderHistory2(Integer customerId) {
        logger.info("[ OrderService#findOrderHistory2 ]");

        // 顧客IDから注文履歴のリストを取得し、返す
        // DAOロジックを直接実装
        logger.info("[ OrderTranDao#findOrderHistoryByCustomerId ]");
        TypedQuery<OrderHistoryTO> query = em.createQuery(
                "SELECT new pro.kensait.berrybooks.service.order.OrderHistoryTO(" +
                "ot.orderDate, ot.orderTranId, od.orderDetailId, " +
                "b.bookName, p.publisherName, b.price, od.count) " +
                "FROM OrderTran ot " +
                "INNER JOIN ot.orderDetails od " +
                "INNER JOIN od.book b " +
                "INNER JOIN b.publisher p " +
                "WHERE ot.customerId = :customerId " +
                "ORDER BY ot.orderDate DESC, ot.orderTranId DESC",
                OrderHistoryTO.class);
        query.setParameter("customerId", customerId);
        
        List<OrderHistoryTO> orderHistoryList = query.getResultList();
        return orderHistoryList;
    }

    // サービスメソッド：注文エンティティのリストを取得する（方式3）
    @Override
    public List<OrderTran> getOrderHistory3(Integer customerId) {
        logger.info("[ OrderService#findOrderHistory3 ]");

        // 顧客IDから注文エンティティのリストを取得し、返す
        // DAOロジックを直接実装
        logger.info("[ OrderTranDao#findByCustomerIdWithDetails ]");
        TypedQuery<OrderTran> query = em.createQuery(
                "SELECT DISTINCT ot FROM OrderTran ot " +
                "LEFT JOIN FETCH ot.orderDetails od " +
                "LEFT JOIN FETCH od.book " +
                "WHERE ot.customerId = :customerId " +
                "ORDER BY ot.orderDate DESC, ot.orderTranId DESC",
                OrderTran.class);
        query.setParameter("customerId", customerId);
        
        List<OrderTran> orderTranList = query.getResultList();
        return orderTranList;
    }

    // サービスメソッド：注文エンティティを取得する
    @Override
    public OrderTran getOrderTran(Integer orderTranId) {
        logger.info("[ OrderService#getOrderTran ]");

        // 注文IDから注文エンティティを取得し、返す
        // DAOロジックを直接実装
        logger.info("[ OrderTranDao#findById ]");
        OrderTran orderTran = em.find(OrderTran.class, orderTranId);
        
        if (orderTran == null) {
            throw new RuntimeException(MessageUtil.get("error.order-tran.not-found") + orderTranId);
        }
        return orderTran;
    }

    // サービスメソッド：注文エンティティを明細と共に取得する
    @Override
    public OrderTran getOrderTranWithDetails(Integer orderTranId) {
        logger.info("[ OrderService#getOrderTranWithDetails ]");

        // 注文IDから注文エンティティを明細と共に取得し、返す
        // DAOロジックを直接実装
        logger.info("[ OrderTranDao#findByIdWithDetails ] orderTranId=" + orderTranId);
        
        // EntityManagerをクリアしてキャッシュをクリア
        em.clear();
        
        // OrderTranを取得（EAGERロードにより明細も自動的に取得される）
        OrderTran orderTran = em.find(OrderTran.class, orderTranId);
        
        if (orderTran == null) {
            throw new RuntimeException(MessageUtil.get("error.order-tran.not-found") + orderTranId);
        }
        return orderTran;
    }

    // サービスメソッド：注文明細エンティティを取得する
    @Override
    public OrderDetail getOrderDetail(OrderDetailPK pk) {
        logger.info("[ OrderService#getOrderDetail ]");

        // 複合主キー（注文IDと注文明細ID）から注文明細エンティティを取得し、返す
        // DAOロジックを直接実装
        logger.info("[ OrderDetailDao#findById ]");
        OrderDetail orderDetail = em.find(OrderDetail.class, pk);
        
        if (orderDetail == null) {
            throw new RuntimeException(MessageUtil.get("error.order-detail.not-found") + pk);
        }
        return orderDetail;
    }

    // サービスメソッド：注文明細エンティティを取得する（オーバーロード）
    @Override
    public OrderDetail getOrderDetail(Integer tranId, Integer detailId) {
        logger.info("[ OrderService#getOrderDetail ] tranId=" + tranId 
                + ", detailId=" + detailId);

        OrderDetailPK pk = new OrderDetailPK(tranId, detailId);
        return getOrderDetail(pk);
    }

    // サービスメソッド：注文明細エンティティのリストを取得する
    @Override
    public List<OrderDetail> getOrderDetails(Integer orderTranId) {
        logger.info("[ OrderService#getOrderDetails ]");

        // 注文IDから注文明細エンティティのリストを取得し、返す
        // DAOロジックを直接実装
        logger.info("[ OrderDetailDao#findByOrderTranId ]");
        TypedQuery<OrderDetail> query = em.createQuery(
                "SELECT od FROM OrderDetail od WHERE od.orderTranId = :orderTranId",
                OrderDetail.class);
        query.setParameter("orderTranId", orderTranId);
        
        List<OrderDetail> orderDetailList = query.getResultList();
        return orderDetailList;
    }

    // サービスメソッド：注文する
    @Override
    public OrderTran orderBooks(OrderTO orderTO) {
        logger.info("[ OrderService#orderBooks ]");

        // カートに追加された書籍毎に、在庫の残り個数をチェックする
        for (CartItem cartItem : orderTO.cartItems()) {

            // 楽観的ロック：カート追加時点のVERSION値で在庫エンティティを作成
            // （悲観的ロックのfindByIdWithLockは使用しない）
            Stock stock = new Stock();
            stock.setBookId(cartItem.getBookId());
            stock.setVersion(cartItem.getVersion());  // カート追加時点のVERSION値を使用
            
            // 現在の在庫数を取得（楽観的ロックなし）
            // DAOロジックを直接実装
            logger.info("[ StockDao#findById ]");
            Stock currentStock = em.find(Stock.class, cartItem.getBookId());

            // 在庫が0未満になる場合は、例外を送出する
            int remaining = currentStock.getQuantity() - cartItem.getCount();
            if (remaining < 0) {
                throw new OutOfStockException(
                        cartItem.getBookId(),
                        cartItem.getBookName(),
                        MessageUtil.get("error.out-of-stock.message"));
            }

            // 在庫を減らす（カート追加時点のVERSION値を持つStockエンティティで更新）
            stock.setQuantity(remaining);
            // JPAの@Versionアノテーションにより、UPDATE時に自動的にバージョンチェックされる
            // WHERE VERSION = ? 条件が付加され、バージョン不一致の場合はOptimisticLockExceptionがスローされる
            // DAOロジックを直接実装
            logger.info("[ StockDao#update ]");
            em.merge(stock);
        }

        // 新しいOrderTranインスタンスを生成する
        OrderTran orderTran = new OrderTran(
                orderTO.orderDate(),
                orderTO.customerId(),
                orderTO.totalPrice(),
                orderTO.deliveryPrice(),
                orderTO.deliveryAddress(),
                orderTO.settlementType());

        // 生成したOrderTranインスタンスをpersist操作により永続化する
        // DAOロジックを直接実装
        logger.info("[ OrderTranDao#persist ]");
        em.persist(orderTran);
        // IDENTITYストラテジーでIDを確実に生成するためにflush
        em.flush();

        // カートアイテム（個々の注文明細）のイテレータを取得する
        List<CartItem> cartItems = orderTO.cartItems();

        // OrderDetailインスタンスの主キー値（注文明細ID）の初期値を設定する
        int orderDetailId = 0;

        for (CartItem cartItem : cartItems) {
            // DAOロジックを直接実装
            logger.info("[ BookDao#findById ]");
            Book book = em.find(Book.class, cartItem.getBookId());

            // OrderDetailインスタンスの主キー値（注文明細ID）をカウントアップする
            orderDetailId = orderDetailId + 1;

            // 新しいOrderDetailインスタンスを生成する
            OrderDetail orderDetail = new OrderDetail(
                    orderTran.getOrderTranId(),
                    orderDetailId,
                    book,
                    cartItem.getCount());

            // OrderDetailインスタンスを保存する
            // DAOロジックを直接実装
            logger.info("[ OrderDetailDao#persist ]");
            em.persist(orderDetail);
            // 即座にINSERTを実行してデータベースに反映
            em.flush();
        }

        // データベースから明細を含めて再取得して返す
        // （永続化した明細をorderDetailsリレーションシップに反映させるため）
        // DAOロジックを直接実装
        logger.info("[ OrderTranDao#findByIdWithDetails ] orderTranId=" + orderTran.getOrderTranId());
        
        // EntityManagerをクリアしてキャッシュをクリア
        em.clear();
        
        // OrderTranを取得（EAGERロードにより明細も自動的に取得される）
        return em.find(OrderTran.class, orderTran.getOrderTranId());
    }
}