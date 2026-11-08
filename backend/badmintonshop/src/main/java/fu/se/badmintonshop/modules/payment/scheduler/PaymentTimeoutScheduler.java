package fu.se.badmintonshop.modules.payment.scheduler;

import fu.se.badmintonshop.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentService paymentService;

    /**
     * Chạy định kỳ mỗi 60 giây để quét các phiên thanh toán pending quá thời gian hiệu lực cài đặt,
     * tự động chuyển trạng thái sang expired và hủy đơn hàng tương ứng.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void scanAndExpirePendingPayments() {
        try {
            int expiredCount = paymentService.processExpiredPayments();
            if (expiredCount > 0) {
                log.info("PaymentTimeoutScheduler: Đã xử lý hết hạn {} phiên thanh toán quá hạn cấu hình.", expiredCount);
            }
        } catch (Exception e) {
            log.warn("PaymentTimeoutScheduler gặp lỗi khi xử lý hết hạn thanh toán: {}", e.getMessage());
        }
    }
}

