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
public class ProductImageResponse {
    private Long id;
    @JsonProperty("product_id")
    private Long productId;
    @JsonProperty("variant_id")
    private Long variantId;
    private String url;
    @JsonProperty("sort_order")
    private Integer sortOrder;
}
