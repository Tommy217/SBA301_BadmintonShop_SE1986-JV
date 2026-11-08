package fu.se.badmintonshop.modules.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("order_code")
    private String orderCode;

    @JsonProperty("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    @JsonProperty("voucher_id")
    private Integer voucherId;

    @JsonProperty("voucher_code")
    private String voucherCode;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("status")
    private String status;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("receiver_name")
    private String receiverName;

    @JsonProperty("receiver_phone")
    private String receiverPhone;

    @JsonProperty("shipping_address")
    private String shippingAddress;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @JsonProperty("discount_amount")
    private BigDecimal discountAmount;

    @JsonProperty("shipping_fee")
    private BigDecimal shippingFee;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("note")
    private String note;

    @JsonProperty("confirmed_by")
    private Long confirmedBy;

    @JsonProperty("confirmed_at")
    private LocalDateTime confirmedAt;

    @JsonProperty("cancelled_by")
    private Long cancelledBy;

    @JsonProperty("cancelled_at")
    private LocalDateTime cancelledAt;

    @JsonProperty("cancel_reason")
    private String cancelReason;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("can_cancel")
    private Boolean canCancel;
}
