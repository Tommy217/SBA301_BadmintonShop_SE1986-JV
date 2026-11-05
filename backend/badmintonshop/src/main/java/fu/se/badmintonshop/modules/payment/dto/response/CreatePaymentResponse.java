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
public class CreatePaymentResponse {
    private Long paymentId;
    private String paymentCode;
    private Long payosOrderCode;
    private String checkoutUrl;
    private String qrCodeUrl;
    private BigDecimal amount;
    private String status;
    private LocalDateTime expiresAt;
}

