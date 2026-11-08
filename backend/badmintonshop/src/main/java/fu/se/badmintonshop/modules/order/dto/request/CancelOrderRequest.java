package fu.se.badmintonshop.modules.order.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequest {
    @JsonProperty("reason")
    private String reason;
}
