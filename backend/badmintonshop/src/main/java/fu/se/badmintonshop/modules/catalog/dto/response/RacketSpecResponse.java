package fu.se.badmintonshop.modules.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RacketSpecResponse {
    @JsonProperty("product_id")
    private Long productId;
    @JsonProperty("balance_type")
    private String balanceType;
    private String flex;
    @JsonProperty("play_style")
    private String playStyle;
    @JsonProperty("max_tension_lbs")
    private BigDecimal maxTensionLbs;
    private String material;
    @JsonProperty("shaft_diameter")
    private BigDecimal shaftDiameter;
}
