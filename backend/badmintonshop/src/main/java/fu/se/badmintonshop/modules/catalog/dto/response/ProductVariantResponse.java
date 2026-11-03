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
public class ProductVariantResponse {
    private Long id;
    @JsonProperty("product_id")
    private Long productId;
    private String sku;
    private String barcode;
    private String color;
    private String size;
    private BigDecimal price;
    @JsonProperty("cost_price")
    private BigDecimal costPrice;
    @JsonProperty("stock_quantity")
    private Integer stockQuantity;
    @JsonProperty("low_stock_threshold")
    private Integer lowStockThreshold;
    @JsonProperty("weight_grams")
    private Integer weightGrams;
    @JsonProperty("is_active")
    private Boolean isActive;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
