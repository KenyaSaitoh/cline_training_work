package pro.kensait.berrybooks.dto;

import java.math.BigDecimal;

public record OrderItemTO (
        // 注文明細ID
        Integer orderDetailId,
        // 書籍ID
        Integer bookId,
        // 書籍名
        String bookName,
        // 著者
        String author,
        // 価格（購入時点）
        BigDecimal price,
        // 数量
        Integer count) {
}

