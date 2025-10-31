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
import pro.kensait.berrybooks.common.MessageUtil;
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
    @NotBlank(message = "{error.customer.name.required}")
    @Size(max = 50, message = "{error.customer.name.max-length}")
    private String customerName;
    
    @NotBlank(message = "{error.email.required}")
    @Email(message = "{error.email.invalid}")
    @Size(max = 100, message = "{error.email.max-length}")
    private String email;
    
    @NotBlank(message = "{error.password.required}")
    @Size(min = 8, max = 20, message = "{error.password.length}")
    private String password;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$|^$", 
             message = "{error.birthday.format}")
    private String birthday; // yyyy-MM-dd形式の文字列
    
    @Size(max = 200, message = "{error.address.max-length}")
    private String address;

    // 顧客登録処理
    // ※基本的なバリデーションはBean Validationで自動的に実行される
    public String register() {
        logger.info("[ CustomerBean#register ]");

        try {
            // 住所に対する入力チェック（正しい都道府県名で始まっているか）
            if (address != null && !address.isBlank() && !AddressUtil.startsWithValidPrefecture(address)) {
                logger.info("[ CustomerBean#register ] 住所入力エラー");
                addErrorMessage(MessageUtil.get("error.address.invalid-prefecture"));
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
                    addErrorMessage(MessageUtil.get("error.birthday.parse-error"));
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
            addErrorMessage(MessageUtil.get("error.registration"));
            return null;
        }
    }

    // エラーメッセージを追加
    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    // アクセサメソッド
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

