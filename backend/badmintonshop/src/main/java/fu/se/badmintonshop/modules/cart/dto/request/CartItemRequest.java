package fu.se.badmintonshop.modules.cart.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequest {
    @JsonProperty("variant_id")
    private String variantId;

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("is_selected")
    private Boolean isSelected;
}
