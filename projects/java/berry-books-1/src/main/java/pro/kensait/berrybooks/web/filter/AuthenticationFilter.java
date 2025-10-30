package pro.kensait.berrybooks.web.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.web.login.LoginBean;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 認証チェックフィルタ（ログインしていないユーザーをindex.xhtmlにリダイレクトする）
@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {"*.xhtml"})
public class AuthenticationFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Inject
    private LoginBean loginBean;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // リクエストされたページのパスを取得
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        logger.debug("AuthenticationFilter - RequestURI: {}", requestURI);
        
        // 認証不要なページ（公開ページ）のリスト
        boolean isPublicPage = requestURI.endsWith("/index.xhtml") 
                || requestURI.endsWith("/customerInput.xhtml")
                || requestURI.endsWith("/customerOutput.xhtml")
                || requestURI.contains("/jakarta.faces.resource/");  // JSF リソース（CSS、画像など）
        
        // LoginBeanから直接ログイン状態をチェック（CDIインジェクション経由）
        boolean isLoggedIn = (loginBean != null && loginBean.isLoggedIn());
        
        logger.debug("isPublicPage: {}, isLoggedIn: {}, loginBean: {}", 
                isPublicPage, isLoggedIn, loginBean);
        
        // ログインが必要なページで未ログインの場合、index.xhtml にリダイレクト
        if (!isPublicPage && !isLoggedIn) {
            logger.info("未ログインユーザーをリダイレクト: {} -> {}/index.xhtml", 
                    requestURI, contextPath);
            httpResponse.sendRedirect(contextPath + "/index.xhtml");
        } else {
            // 認証OK、または認証不要なページ → 処理を続行
            chain.doFilter(request, response);
        }
    }
}

