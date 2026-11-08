package fu.se.badmintonshop.modules.order.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    @JsonProperty("source")
    private String source;

    @JsonProperty("variant_id")
    private String variantId;

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("cart_item_ids")
    private List<String> cartItemIds;

    @JsonProperty("receiver_name")
    private String receiverName;

    @JsonProperty("receiver_phone")
    private String receiverPhone;

    @JsonProperty("shipping_address")
    private String shippingAddress;

    @JsonProperty("note")
    private String note;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("voucher_code")
    private String voucherCode;
}
