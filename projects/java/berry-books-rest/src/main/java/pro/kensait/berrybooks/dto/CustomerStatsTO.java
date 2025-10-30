package pro.kensait.berrybooks.dto;

import java.time.LocalDate;

public record CustomerStatsTO (
        // 顧客ID
        Integer customerId,
        // 顧客名
        String customerName,
        // メールアドレス
        String email,
        // 生年月日
        LocalDate birthday,
        // 住所
        String address,
        // 注文件数
        Long orderCount,
        // 購入冊数（合計）
        Long totalBooks) {
}

