package fu.se.badmintonshop.modules.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShoeSpecResponse {
    @JsonProperty("product_id")
    private Long productId;
    private String gender;
    @JsonProperty("sole_type")
    private String soleType;
    private String technology;
    @JsonProperty("foot_width")
    private String footWidth;
}
