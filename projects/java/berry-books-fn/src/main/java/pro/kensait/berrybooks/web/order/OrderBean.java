package pro.kensait.berrybooks.web.order;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.kensait.berrybooks.common.MessageUtil;
import pro.kensait.berrybooks.entity.Customer;
import pro.kensait.berrybooks.entity.OrderDetail;
import pro.kensait.berrybooks.entity.OrderTran;
import pro.kensait.berrybooks.service.delivery.DeliveryFeeService;
import pro.kensait.berrybooks.service.order.OrderHistoryTO;
import pro.kensait.berrybooks.service.order.OrderServiceIF;
import pro.kensait.berrybooks.service.order.OrderTO;
import pro.kensait.berrybooks.service.order.OutOfStockException;
import pro.kensait.berrybooks.util.AddressUtil;
import pro.kensait.berrybooks.web.cart.CartSession;
import pro.kensait.berrybooks.web.customer.CustomerBean;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.OptimisticLockException;

// 注文処理と注文履歴表示のバッキングBean
@Named
@ViewScoped
public class OrderBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(
            OrderBean.class);

    @Inject
    private OrderServiceIF orderService;

    @Inject
    private CustomerBean customerBean;

    @Inject
    private CartSession cartSession;

    @Inject
    private DeliveryFeeService deliveryFeeService;

    // 注文履歴
    private List<OrderHistoryTO> orderHistory;
    private List<OrderHistoryTO> orderHistoryList;
    private List<OrderTran> orderList;

    // 注文詳細
    private OrderTran selectedOrderTran;
    private OrderTran orderTran;
    private List<OrderDetail> orderDetails;
    private OrderDetail orderDetail;

    // ビューパラメータ
    private Integer selectedTranId;
    private Integer selectedDetailId;
    private Integer orderTranId; // 注文成功画面用

    // エラーメッセージ
    private String errorMessage;

    // アクション：注文を確定（方式1）
    public String placeOrder1() {
        logger.info("[ OrderBean#placeOrder1 ]");
        return placeOrderInternal();
    }

    // アクション：注文を確定（方式2）
    public String placeOrder2() {
        logger.info("[ OrderBean#placeOrder2 ]");
        return placeOrderInternal();
    }

    // 内部メソッド：注文を確定
    // ※基本的なバリデーションはBean Validation（CartSession）で自動的に実行される
    private String placeOrderInternal() {
        try {
            // 配送先住所の都道府県をチェックする
            if (cartSession.getDeliveryAddress() != null && 
                    !cartSession.getDeliveryAddress().isBlank() && 
                    !AddressUtil.startsWithValidPrefecture(cartSession.getDeliveryAddress())) {
                logger.info("[ OrderBean#placeOrderInternal ] 配送先住所入力エラー");
                errorMessage = MessageUtil.get("error.delivery-address.invalid-prefecture");
                setFlashErrorMessage(errorMessage);
                return "orderError?faces-redirect=true";
            }

            // 配送料金を再計算する（配送先住所が変更されている可能性があるため）
            BigDecimal deliveryPrice = deliveryFeeService.calculateDeliveryFee(
                    cartSession.getDeliveryAddress(), 
                    cartSession.getTotalPrice());
            cartSession.setDeliveryPrice(deliveryPrice);

            // ログイン中の顧客IDを取得
            Customer customer = customerBean.getCustomer();
            Integer customerId = customer.getCustomerId();

            OrderTO orderTO = new OrderTO(
                    customerId,
                    LocalDate.now(),
                    new ArrayList<>(cartSession.getCartItems()),
                    cartSession.getTotalPrice(),
                    cartSession.getDeliveryPrice(),
                    cartSession.getDeliveryAddress(),
                    cartSession.getSettlementType());

            orderTran = orderService.orderBooks(orderTO);

            // HTTPセッションからカートを削除
            cartSession.getCartItems().clear();
            cartSession.setTotalPrice(BigDecimal.ZERO);
            cartSession.setDeliveryPrice(BigDecimal.ZERO);
            cartSession.setDeliveryAddress(null);
            cartSession.setSettlementType(null);

            // 注文IDをURLパラメータとして渡す
            return "orderSuccess?faces-redirect=true&orderTranId=" + orderTran.getOrderTranId();

        } catch (OutOfStockException e) {
            logger.error("在庫不足エラー", e);
            errorMessage = MessageUtil.get("error.out-of-stock") + e.getBookName();
            setFlashErrorMessage(errorMessage);
            return "orderError?faces-redirect=true";

        } catch (OptimisticLockException e) {
            logger.error("楽観的ロックエラー", e);
            errorMessage = MessageUtil.get("error.optimistic-lock");
            setFlashErrorMessage(errorMessage);
            return "orderError?faces-redirect=true";
        } catch (Exception e) {
            logger.error("注文エラー", e);
            errorMessage = MessageUtil.get("error.order-processing") + e.getMessage();
            setFlashErrorMessage(errorMessage);
            return "orderError?faces-redirect=true";
        }
    }

    // アクション：注文履歴を取得（方式1）
    public void loadOrderHistory() {
        logger.info("[ OrderBean#loadOrderHistory ]");
        Integer customerId = getCustomerId();
        orderHistoryList = orderService.getOrderHistory2(customerId);
    }

    // アクション：注文履歴を取得（方式2）
    public void loadOrderHistory2() {
        logger.info("[ OrderBean#loadOrderHistory2 ]");
        Integer customerId = getCustomerId();
        orderHistoryList = orderService.getOrderHistory2(customerId);
    }

    // アクション：注文履歴を取得（方式3）
    public void loadOrderHistory3() {
        logger.info("[ OrderBean#loadOrderHistory3 ]");
        Integer customerId = getCustomerId();
        orderList = orderService.getOrderHistory3(customerId);
    }

    // アクション：注文詳細を取得
    public void loadOrderDetail() {
        logger.info("[ OrderBean#loadOrderDetail ] tranId=" + selectedTranId 
                + ", detailId=" + selectedDetailId);
        
        if (selectedTranId != null && selectedDetailId != null) {
            orderDetail = orderService.getOrderDetail(selectedTranId, selectedDetailId);
        }
    }

    // アクション：注文詳細を表示
    public String showOrderDetail(Integer orderTranId) {
        logger.info("[ OrderBean#showOrderDetail ] orderTranId=" + orderTranId);
        
        selectedOrderTran = orderService.getOrderTran(orderTranId);
        orderDetails = orderService.getOrderDetails(orderTranId);
        
        return "orderDetail"; // 注文詳細画面へ
    }

    // ヘルパー：顧客IDを取得
    private Integer getCustomerId() {
        Customer customer = customerBean.getCustomer();
        return customer.getCustomerId();
    }

    // FlashScopeにエラーメッセージを設定
    // ※リダイレット後もメッセージを保持するため
    private void setFlashErrorMessage(String message) {
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .getFlash()
                .put("errorMessage", message);
    }

    // アクセサメソッド（CartSessionへの委譲）
    public String getDeliveryAddress() {
        return cartSession.getDeliveryAddress();
    }

    public void setDeliveryAddress(String deliveryAddress) {
        cartSession.setDeliveryAddress(deliveryAddress);
    }

    public Integer getSettlementType() {
        return cartSession.getSettlementType();
    }

    public void setSettlementType(Integer settlementType) {
        cartSession.setSettlementType(settlementType);
    }

    public List<OrderHistoryTO> getOrderHistory() {
        return orderHistory;
    }

    public List<OrderHistoryTO> getOrderHistoryList() {
        return orderHistoryList;
    }

    public List<OrderTran> getOrderList() {
        return orderList;
    }

    public OrderTran getSelectedOrderTran() {
        return selectedOrderTran;
    }

    public OrderTran getOrderTran() {
        return orderTran;
    }

    public List<OrderDetail> getOrderDetails() {
        return orderDetails;
    }

    public OrderDetail getOrderDetail() {
        return orderDetail;
    }

    public Integer getSelectedTranId() {
        return selectedTranId;
    }

    public void setSelectedTranId(Integer selectedTranId) {
        this.selectedTranId = selectedTranId;
    }

    public Integer getSelectedDetailId() {
        return selectedDetailId;
    }

    public void setSelectedDetailId(Integer selectedDetailId) {
        this.selectedDetailId = selectedDetailId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getOrderTranId() {
        return orderTranId;
    }

    public void setOrderTranId(Integer orderTranId) {
        this.orderTranId = orderTranId;
    }

    // アクション：注文成功画面用にデータをロード
    public void loadOrderSuccess() {
        logger.info("[ OrderBean#loadOrderSuccess ] orderTranId=" + orderTranId);
        if (orderTranId != null) {
            orderTran = orderService.getOrderTranWithDetails(orderTranId);
        }
    }
}

