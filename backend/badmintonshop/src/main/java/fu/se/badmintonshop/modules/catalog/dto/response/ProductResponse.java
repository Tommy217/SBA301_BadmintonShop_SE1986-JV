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
public class ProductResponse {
    private Long id;
    @JsonProperty("category_id")
    private Integer categoryId;
    @JsonProperty("brand_id")
    private Integer brandId;
    private String brand;
    @JsonProperty("product_type")
    private String productType;
    private String name;
    private String slug;
    private String description;
    @JsonProperty("base_price")
    private BigDecimal basePrice;
    @JsonProperty("is_active")
    private Boolean isActive;
    @JsonProperty("created_by")
    private Long createdBy;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
