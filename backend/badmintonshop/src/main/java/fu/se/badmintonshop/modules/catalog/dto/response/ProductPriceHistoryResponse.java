package fu.se.badmintonshop.modules.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductPriceHistoryResponse {
    private Long id;
    @JsonProperty("variant_id")
    private Long variantId;
    @JsonProperty("old_price")
    private BigDecimal oldPrice;
    @JsonProperty("new_price")
    private BigDecimal newPrice;
    @JsonProperty("changed_by")
    private Long changedBy;
    @JsonProperty("changed_at")
    private LocalDateTime changedAt;
}
