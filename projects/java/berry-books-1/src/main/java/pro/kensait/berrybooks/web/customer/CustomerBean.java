package pro.kensait.berrybooks.web.customer;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pro.kensait.berrybooks.common.ErrorMessage;
import pro.kensait.berrybooks.entity.Customer;
import pro.kensait.berrybooks.service.customer.CustomerService;
import pro.kensait.berrybooks.service.customer.EmailAlreadyExistsException;
import pro.kensait.berrybooks.util.AddressUtil;

// 顧客登録画面のバッキングBean
@Named
@SessionScoped
public class CustomerBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(
            CustomerBean.class);

    @Inject
    private CustomerService customerService;

    // 現在ログイン中の顧客
    private Customer customer;

    // 登録フォームの入力値
    @NotBlank(message = ErrorMessage.CUSTOMER_NAME_REQUIRED)
    @Size(max = 50, message = ErrorMessage.CUSTOMER_NAME_MAX_LENGTH)
    private String customerName;
    
    @NotBlank(message = ErrorMessage.EMAIL_REQUIRED)
    @Email(message = ErrorMessage.EMAIL_INVALID)
    @Size(max = 100, message = ErrorMessage.EMAIL_MAX_LENGTH)
    private String email;
    
    @NotBlank(message = ErrorMessage.PASSWORD_REQUIRED)
    @Size(min = 8, max = 20, message = ErrorMessage.PASSWORD_LENGTH)
    private String password;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$|^$", 
             message = ErrorMessage.BIRTHDAY_FORMAT)
    private String birthday; // yyyy-MM-dd形式の文字列
    
    @Size(max = 200, message = ErrorMessage.ADDRESS_MAX_LENGTH)
    private String address;

    // 顧客登録処理
    // ※基本的なバリデーションはBean Validationで自動的に実行される
    public String register() {
        logger.info("[ CustomerBean#register ]");

        try {
            // 住所に対する入力チェック（正しい都道府県名で始まっているか）
            if (address != null && !address.isBlank() && !AddressUtil.startsWithValidPrefecture(address)) {
                logger.info("[ CustomerBean#register ] 住所入力エラー");
                addErrorMessage(ErrorMessage.ADDRESS_INVALID_PREFECTURE);
                return null;
            }

            // Customerエンティティを生成
            Customer newCustomer = new Customer();
            newCustomer.setCustomerName(customerName);
            newCustomer.setEmail(email);
            newCustomer.setPassword(password);
            
            // 生年月日のパース（@Patternで形式チェック済み）
            if (birthday != null && !birthday.isEmpty()) {
                try {
                    LocalDate birthDate = LocalDate.parse(birthday, 
                            DateTimeFormatter.ISO_LOCAL_DATE);
                    newCustomer.setBirthday(birthDate);
                } catch (Exception e) {
                    logger.warn("Birthday parse error: " + birthday, e);
                    addErrorMessage(ErrorMessage.BIRTHDAY_PARSE_ERROR);
                    return null;
                }
            }
            
            newCustomer.setAddress(address);

            // 顧客登録
            customer = customerService.registerCustomer(newCustomer);

            logger.info("Customer registered: " + customer);

            // 登録完了ページへ遷移
            return "customerOutput?faces-redirect=true";

        } catch (EmailAlreadyExistsException e) {
            logger.error("Email already exists: " + e.getEmail(), e);
            addErrorMessage(e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Registration error", e);
            addErrorMessage(ErrorMessage.REGISTRATION_ERROR);
            return null;
        }
    }

    // エラーメッセージを追加
    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    // Getters and Setters
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

