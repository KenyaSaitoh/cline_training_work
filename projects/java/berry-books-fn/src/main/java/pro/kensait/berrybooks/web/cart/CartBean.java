package pro.kensait.berrybooks.web.cart;

import java.io.Serializable;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.common.MessageUtil;
import pro.kensait.berrybooks.dao.StockDao;
import pro.kensait.berrybooks.entity.Book;
import pro.kensait.berrybooks.entity.Stock;
import pro.kensait.berrybooks.service.book.BookService;
import pro.kensait.berrybooks.service.delivery.DeliveryFeeService;
import pro.kensait.berrybooks.web.customer.CustomerBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

// ショッピングカート操作のバッキングBean
@Named
@SessionScoped
public class CartBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(
            CartBean.class);

    @Inject
    private BookService bookService;

    @Inject
    private StockDao stockDao;

    @Inject
    private CartSession cartSession;

    @Inject
    private CustomerBean customerBean;

    @Inject
    private DeliveryFeeService deliveryFeeService;

    // アクション：書籍をカートに追加
    public String addBook(Integer bookId, Integer count) {
        logger.info("[ CartBean#addBook ] bookId=" + bookId + ", count=" + count);

        Book book = bookService.getBook(bookId);
        
        // 楽観的ロック用：Stockエンティティから現在のVERSION値を取得
        Stock stock = stockDao.findById(bookId);
        logger.info("[ CartBean#addBook ] Stock version=" + stock.getVersion());

        // 選択された書籍がカートに存在している場合は、注文数と金額を加算する
        boolean isExists = false;
        for (CartItem cartItem : cartSession.getCartItems()) {
            if (bookId.equals(cartItem.getBookId())) {
                cartItem.setCount(cartItem.getCount() + count);
                cartItem.setPrice(cartItem.getPrice().add(book.getPrice().multiply(BigDecimal.valueOf(count))));
                // VERSION値は最初にカートに入れた時点のものを保持（更新しない）
                isExists = true;
                break;
            }
        }

        // 選択された書籍がカートに存在していない場合は、新しいCartItemを生成しカートに追加する
        if (!isExists) {
            CartItem cartItem = new CartItem(
                    book.getBookId(),
                    book.getBookName(),
                    book.getPublisher().getPublisherName(),
                    book.getPrice().multiply(BigDecimal.valueOf(count)),
                    count,
                    false);
            // カート追加時点のVERSION値をCartItemに保存
            cartItem.setVersion(stock.getVersion());
            cartSession.getCartItems().add(cartItem);
        }

        // 合計金額を加算する
        BigDecimal totalPrice = cartSession.getTotalPrice();
        cartSession.setTotalPrice(totalPrice.add(book.getPrice().multiply(BigDecimal.valueOf(count))));

        return "cartView?faces-redirect=true";
    }

    // アクション：選択した書籍をカートから削除
    public String removeSelectedBooks() {
        logger.info("[ CartBean#removeSelectedBooks ]");
        
        // 選択された書籍を削除し、合計金額を再計算
        cartSession.getCartItems().removeIf(item -> {
            if (item.isRemove()) {
                BigDecimal totalPrice = cartSession.getTotalPrice();
                cartSession.setTotalPrice(totalPrice.subtract(item.getPrice()));
                return true;
            }
            return false;
        });
        
        return null;
    }

    // アクション：カートをクリア
    public String clearCart() {
        logger.info("[ CartBean#clearCart ]");
        cartSession.getCartItems().clear();
        cartSession.setTotalPrice(BigDecimal.ZERO);
        cartSession.setDeliveryPrice(BigDecimal.ZERO);
        return "cartClear?faces-redirect=true";
    }

    // アクション：カートの内容を確定する
    public String proceedToOrder() {
        logger.info("[ CartBean#proceedToOrder ]");
        
        if (cartSession.getCartItems().isEmpty()) {
            return null;
        }

        // デフォルトの配送先住所として、顧客の住所を設定する
        if (customerBean.getCustomer() != null) {
            cartSession.setDeliveryAddress(customerBean.getCustomer().getAddress());
        }

        // 配送料金を計算する
        // ※通常800円、沖縄県は1700円、5000円以上は送料無料
        BigDecimal deliveryPrice = deliveryFeeService.calculateDeliveryFee(
                cartSession.getDeliveryAddress(), 
                cartSession.getTotalPrice());
        cartSession.setDeliveryPrice(deliveryPrice);

        return "bookOrder?faces-redirect=true";
    }

    // アクション：カートを参照する
    public String viewCart() {
        logger.info("[ CartBean#viewCart ]");

        // カートに商品が一つも入っていなかった場合は、エラーメッセージを設定
        if (cartSession.getCartItems().isEmpty()) {
            logger.info("[ CartBean#viewCart ] カートに商品なしエラー");
            globalErrorMessage = MessageUtil.get("error.cart.empty");
        }

        return "cartView?faces-redirect=true";
    }

    // グローバルエラーメッセージ
    private String globalErrorMessage;

    public String getGlobalErrorMessage() {
        return globalErrorMessage;
    }

    public void setGlobalErrorMessage(String globalErrorMessage) {
        this.globalErrorMessage = globalErrorMessage;
    }

    // アクセサメソッド（CartSessionへの委譲）
    public BigDecimal getTotalPrice() {
        return cartSession.getTotalPrice();
    }

    public BigDecimal getDeliveryPrice() {
        return cartSession.getDeliveryPrice();
    }

    public boolean isCartEmpty() {
        return cartSession.getCartItems().isEmpty();
    }
}

