package pro.kensait.berrybooks.web.cart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// ショッピングカートのセッション情報を管理するクラス
@Named
@SessionScoped
public class CartSession implements Serializable {
    private static final long serialVersionUID = 1L;

    // カートアイテムのリスト
    private List<CartItem> cartItems = new CopyOnWriteArrayList<>();
    
    // 注文金額合計
    private BigDecimal totalPrice = BigDecimal.ZERO;
    
    // 配送料金
    private BigDecimal deliveryPrice = BigDecimal.ZERO;
    
    // 配送先住所
    @NotBlank(message = "配送先住所を入力してください")
    @Size(max = 200, message = "配送先住所は200文字以内で入力してください")
    private String deliveryAddress;
    
    // 決済方法
    @NotNull(message = "決済方法を選択してください")
    private Integer settlementType;

    // 引数の無いコンストラクタ
    public CartSession() {
    }

    // 全フィールドを引数にとるコンストラクタ
    public CartSession(List<CartItem> cartItems, BigDecimal totalPrice,
            BigDecimal deliveryPrice, String deliveryAddress, Integer settlementType) {
        this.cartItems = cartItems;
        this.totalPrice = totalPrice;
        this.deliveryPrice = deliveryPrice;
        this.deliveryAddress = deliveryAddress;
        this.settlementType = settlementType;
    }

    /**
     * 合計金額を再計算する
     */
    public void recalculateTotalPrice() {
        totalPrice = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            totalPrice = totalPrice.add(item.getPrice());
        }
    }

    // アクセサメソッド
    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getDeliveryPrice() {
        return deliveryPrice;
    }

    public void setDeliveryPrice(BigDecimal deliveryPrice) {
        this.deliveryPrice = deliveryPrice;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Integer getSettlementType() {
        return settlementType;
    }

    public void setSettlementType(Integer settlementType) {
        this.settlementType = settlementType;
    }

    @Override
    public String toString() {
        return "CartSession [cartItems=" + cartItems + ", totalPrice=" + totalPrice
                + ", deliveryPrice=" + deliveryPrice + ", deliveryAddress="
                + deliveryAddress + ", settlementType=" + settlementType + "]";
    }
}

