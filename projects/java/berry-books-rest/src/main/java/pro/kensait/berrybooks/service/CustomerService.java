package pro.kensait.berrybooks.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.dao.CustomerDao;
import pro.kensait.berrybooks.dao.OrderTranDao;
import pro.kensait.berrybooks.entity.Customer;
import pro.kensait.berrybooks.entity.OrderTran;
import pro.kensait.berrybooks.exception.CustomerExistsException;
import pro.kensait.berrybooks.exception.CustomerNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

// 顧客情報の取得・登録・更新・削除を行うサービスクラス
@ApplicationScoped
@Transactional
public class CustomerService {
    private static final String CUSTOMER_NOT_FOUND_MESSAGE =
            "指定されたメールアドレスは存在しません";
    private static final String CUSTOMER_EXISTS_MESSAGE =
            "指定されたメールアドレスはすでに存在します";

    private static final Logger logger = LoggerFactory.getLogger(
            CustomerService.class);

    @Inject
    private CustomerDao customerDao;

    @Inject
    private OrderTranDao orderTranDao;

    // サービスメソッド：顧客を取得する（一意キーからの条件検索）
    public Customer getCustomerById(Integer customerId) {
        logger.info("[ CustomerService#getCustomerById ]");

        // メールアドレスから顧客エンティティを検索する
        Customer customer = customerDao.findById(customerId);
        if (customer == null) {
            throw new CustomerNotFoundException(CUSTOMER_NOT_FOUND_MESSAGE);
        }
        return customer;
    }

    // サービスメソッド：顧客を取得する（一意キーからの条件検索）
    public Customer getCustomerByEmail(String email) {
        logger.info("[ CustomerService#getCustomerByEmail ]");

        // メールアドレスから顧客エンティティを検索する
        Customer customer = customerDao.findCustomerByEmail(email);
        if (customer == null) {
            throw new CustomerNotFoundException(CUSTOMER_NOT_FOUND_MESSAGE);
        }
        return customer;
    }

    // サービスメソッド：顧客リストを取得する（誕生日からの条件検索）
    public List<Customer> searchCustomersFromBirthday(LocalDate from) {
        logger.info("[ CustomerService#searchCustomersFromBirthday ]");

        // 誕生日開始日から顧客エンティティを検索する
        List<Customer> customers = customerDao.searchCustomersFromBirthday(from);
        return customers;
    }

    // サービスメソッド：顧客を新規登録する
    public Customer registerCustomer(Customer customer) throws CustomerExistsException { 
        logger.info("[ CustomerService#registerCustomer ]");

        // メールアドレスの重複チェック
        Customer existing = customerDao.findCustomerByEmail(customer.getEmail());
        if (existing != null) {
            throw new CustomerExistsException(CUSTOMER_EXISTS_MESSAGE);
        }

        // 受け取った顧客エンティティを保存する
        customerDao.persist(customer);
        return customer;
    }

    // サービスメソッド：顧客を上書き登録する
    public void replaceCustomer(Customer customer)
            throws CustomerExistsException { 
        logger.info("[ CustomerService#replaceCustomer ]");

        // 既存の顧客情報を取得
        Customer existingCustomer = customerDao.findById(customer.getCustomerId());
        if (existingCustomer == null) {
            throw new CustomerNotFoundException(CUSTOMER_NOT_FOUND_MESSAGE);
        }

        // パスワード以外のフィールドを更新
        existingCustomer.setCustomerName(customer.getCustomerName());
        existingCustomer.setEmail(customer.getEmail());
        existingCustomer.setBirthday(customer.getBirthday());
        existingCustomer.setAddress(customer.getAddress());
        // パスワードは保持（更新しない）

        // 受け取った顧客エンティティを保存する
        customerDao.merge(existingCustomer);
    }

    // サービスメソッド：顧客を削除する
    public void deleteCustomer(Integer customerId) {
        logger.info("[ CustomerService#deleteCustomer ]");

        // 受け取った顧客IDをキーにエンティティを削除する
        Customer customer = customerDao.findById(customerId);
        if (customer != null) {
            customerDao.remove(customer);
        }
    }

    // サービスメソッド：顧客の注文履歴を取得する
    public List<OrderTran> getOrderHistory(Integer customerId) {
        logger.info("[ CustomerService#getOrderHistory ]");
        
        // 顧客の存在確認
        Customer customer = customerDao.findById(customerId);
        if (customer == null) {
            throw new CustomerNotFoundException(CUSTOMER_NOT_FOUND_MESSAGE);
        }
        
        return orderTranDao.findByCustomerId(customerId);
    }

    // サービスメソッド：全顧客を取得する
    public List<Customer> getAllCustomers() {
        logger.info("[ CustomerService#getAllCustomers ]");
        return customerDao.findAll();
    }

    // サービスメソッド：顧客の注文件数を取得する
    public Long getOrderCount(Integer customerId) {
        logger.info("[ CustomerService#getOrderCount ]");
        return orderTranDao.countOrdersByCustomerId(customerId);
    }

    // サービスメソッド：顧客の購入冊数を取得する
    public Long getTotalBookCount(Integer customerId) {
        logger.info("[ CustomerService#getTotalBookCount ]");
        return orderTranDao.sumBookCountByCustomerId(customerId);
    }
}

