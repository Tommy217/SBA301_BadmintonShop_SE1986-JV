package fu.se.badmintonshop.modules.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailResponse {
    private Long id;
    private Long orderId;
    private String orderCode;
    private String paymentCode;
    private Long payosOrderCode;
    private String method;
    private String gateway;
    private String status;
    private BigDecimal amount;
    private String gatewayTxnRef;
    private String qrCodeUrl;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

