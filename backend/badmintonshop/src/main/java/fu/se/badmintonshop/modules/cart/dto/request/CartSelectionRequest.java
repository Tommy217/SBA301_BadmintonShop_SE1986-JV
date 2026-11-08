package fu.se.badmintonshop.modules.cart.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartSelectionRequest {
    @JsonProperty("selected")
    private Boolean selected;
}
