package fu.se.badmintonshop.modules.order.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {
    @JsonProperty("expected_status")
    private String expectedStatus;

    @JsonProperty("expected_payment_status")
    private String expectedPaymentStatus;

    @JsonProperty("status")
    private String status;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("note")
    private String note;
}
