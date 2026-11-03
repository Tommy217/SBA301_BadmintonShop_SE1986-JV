package fu.se.badmintonshop.modules.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductVariantRequest {
    @JsonProperty("product_id")
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 60, message = "SKU tối đa 60 ký tự")
    private String sku;

    @Size(max = 60, message = "Mã vạch tối đa 60 ký tự")
    private String barcode;

    @Size(max = 50, message = "Màu tối đa 50 ký tự")
    private String color;

    @Size(max = 30, message = "Kích cỡ tối đa 30 ký tự")
    private String size;

    @NotNull(message = "Giá bán không được để trống")
    private BigDecimal price;

    @JsonProperty("cost_price")
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @JsonProperty("stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 0;

    @JsonProperty("low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @JsonProperty("weight_grams")
    @Builder.Default
    private Integer weightGrams = 0;

    @JsonProperty("is_active")
    @Builder.Default
    private Boolean isActive = true;
}
