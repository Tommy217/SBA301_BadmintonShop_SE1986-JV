package fu.se.badmintonshop.modules.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fu.se.badmintonshop.modules.catalog.dto.response.*;
import fu.se.badmintonshop.modules.supportticket.dto.response.*;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartSnapshotResponse {
    @JsonProperty("carts")
    private List<CartResponse> carts;

    @JsonProperty("cart_items")
    private List<CartItemResponse> cartItems;
}
