package pro.kensait.berrybooks.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderHistoryTO (
        // 注文ID
        Integer orderTranId,
        // 注文日
        LocalDate orderDate,
        // 注文金額合計
        BigDecimal totalPrice,
        // 配送料金
        BigDecimal deliveryPrice,
        // 配送先住所
        String deliveryAddress,
        // 決済方法
        Integer settlementType,
        // 注文明細リスト
        List<OrderItemTO> items) {
}

