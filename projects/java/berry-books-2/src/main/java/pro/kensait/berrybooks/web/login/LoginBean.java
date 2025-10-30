package pro.kensait.berrybooks.web.login;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.common.ErrorMessage;
import pro.kensait.berrybooks.entity.Customer;
import pro.kensait.berrybooks.service.customer.CustomerService;
import pro.kensait.berrybooks.web.customer.CustomerBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// ログイン画面のバッキングBean
@Named
@SessionScoped
public class LoginBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(
            LoginBean.class);

    @Inject
    private CustomerService customerService;

    @Inject
    private CustomerBean customerBean;

    // ログインフォームの入力値
    @NotBlank(message = ErrorMessage.EMAIL_REQUIRED)
    @Email(message = ErrorMessage.EMAIL_INVALID)
    private String email;
    
    @NotBlank(message = ErrorMessage.PASSWORD_REQUIRED)
    private String password;

    // ログイン済みフラグ
    private boolean loggedIn = false;

    // ログイン処理
    public String processLogin() {
        logger.info("[ LoginBean#processLogin ] email=" + email);

        try {
            Customer customer = customerService.authenticate(email, password);
            
            if (customer == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                ErrorMessage.LOGIN_FAILED,
                                ErrorMessage.LOGIN_INVALID_CREDENTIALS));
                return null;
            }

            // CustomerBeanに顧客情報を設定
            customerBean.setCustomer(customer);
            loggedIn = true;

            logger.info("Login successful: " + customer.getCustomerName());
            
            // 書籍選択ページへ遷移
            return "bookSelect?faces-redirect=true";

        } catch (Exception e) {
            logger.error("Login error", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            ErrorMessage.GENERAL_ERROR, e.getMessage()));
            return null;
        }
    }

    // ログアウト処理
    public String processLogout() {
        logger.info("[ LoginBean#processLogout ]");
        
        // セッションを無効化
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        
        // トップページへ遷移
        return "index?faces-redirect=true";
    }

    // Getters and Setters
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

    public boolean isLoggedIn() {
        return loggedIn;
    }
}

