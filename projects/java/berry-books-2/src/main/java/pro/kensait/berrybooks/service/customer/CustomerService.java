package pro.kensait.berrybooks.service.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.common.MessageUtil;
import pro.kensait.berrybooks.dao.CustomerDao;
import pro.kensait.berrybooks.entity.Customer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

// 顧客登録と認証を行うサービスクラス
@ApplicationScoped
public class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(
            CustomerService.class);

    @Inject
    private CustomerDao customerDao;

    // 顧客を登録する（メールアドレス重複チェック含む）
    @Transactional
    public Customer registerCustomer(Customer customer) {
        logger.info("[ CustomerService#registerCustomer ]");
        
        // メールアドレスの重複チェック
        Customer existing = customerDao.findByEmail(customer.getEmail());
        if (existing != null) {
            throw new EmailAlreadyExistsException(customer.getEmail(), 
                    MessageUtil.get("error.email.already-exists"));
        }
        
        customerDao.register(customer);
        return customer;
    }

    // ログイン認証を行う
    public Customer authenticate(String email, String password) {
        logger.info("[ CustomerService#authenticate ] email=" + email);
        
        Customer customer = customerDao.findByEmail(email);
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
        return customerDao.findById(customerId);
    }
}

