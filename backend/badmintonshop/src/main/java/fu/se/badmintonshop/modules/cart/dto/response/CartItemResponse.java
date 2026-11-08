package fu.se.badmintonshop.modules.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("cart_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long cartId;

    @JsonProperty("variant_id")
    private Long variantId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("is_selected")
    private Boolean selected;

    @JsonProperty("added_at")
    private LocalDateTime addedAt;
}
