package fu.se.badmintonshop.modules.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Mã đơn hàng (orderId) là bắt buộc")
    private Long orderId;

    private BigDecimal amount;

    private String description;

    private String buyerName;

    private String buyerEmail;

    private String buyerPhone;

    private String returnUrl;

    private String cancelUrl;
}

