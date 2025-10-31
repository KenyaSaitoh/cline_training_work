package pro.kensait.berrybooks.service.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.common.MessageUtil;
import pro.kensait.berrybooks.entity.Customer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

// 顧客登録と認証を行うサービスクラス
@ApplicationScoped
public class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(
            CustomerService.class);

    @PersistenceContext(unitName = "bookstorePU")
    private EntityManager em;

    // 顧客を登録する（メールアドレス重複チェック含む）
    @Transactional
    public Customer registerCustomer(Customer customer) {
        logger.info("[ CustomerService#registerCustomer ]");
        
        // メールアドレスの重複チェック
        // DAOロジックを直接実装
        logger.info("[ CustomerDao#findByEmail ] email=" + customer.getEmail());
        TypedQuery<Customer> query = em.createQuery(
                "SELECT c FROM Customer c WHERE c.email = :email",
                Customer.class);
        query.setParameter("email", customer.getEmail());
        
        Customer existing = null;
        try {
            existing = query.getSingleResult();
        } catch (NoResultException e) {
            existing = null;
        }
        
        if (existing != null) {
            throw new EmailAlreadyExistsException(customer.getEmail(), 
                    MessageUtil.get("error.email.already-exists"));
        }
        
        // DAOロジックを直接実装
        logger.info("[ CustomerDao#register ] customer=" + customer);
        em.persist(customer);
        
        return customer;
    }

    // ログイン認証を行う
    public Customer authenticate(String email, String password) {
        logger.info("[ CustomerService#authenticate ] email=" + email);
        
        // DAOロジックを直接実装
        logger.info("[ CustomerDao#findByEmail ] email=" + email);
        TypedQuery<Customer> query = em.createQuery(
                "SELECT c FROM Customer c WHERE c.email = :email",
                Customer.class);
        query.setParameter("email", email);
        
        Customer customer = null;
        try {
            customer = query.getSingleResult();
        } catch (NoResultException e) {
            customer = null;
        }
        
        if (customer == null) {
            logger.warn("Customer not found: " + email);
            return null;
        }
        
        // 平文でパスワードを検証
        if (!customer.getPassword().equals(password)) {
            logger.warn("Password mismatch for: " + email);
            return null;
        }
        
        return customer;
    }

    // 顧客IDで顧客を取得する
    public Customer getCustomer(Integer customerId) {
        logger.info("[ CustomerService#getCustomer ] customerId=" + customerId);
        
        // DAOロジックを直接実装
        logger.info("[ CustomerDao#findById ] customerId=" + customerId);
        return em.find(Customer.class, customerId);
    }
}

