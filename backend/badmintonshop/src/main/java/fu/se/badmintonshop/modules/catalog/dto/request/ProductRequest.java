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
public class ProductRequest {
    @JsonProperty("category_id")
    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    @JsonProperty("brand_id")
    @NotNull(message = "Thương hiệu không được để trống")
    private Integer brandId;

    @JsonProperty("product_type")
    @NotBlank(message = "Loại sản phẩm không được để trống")
    private String productType;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    private String name;

    @Size(max = 280, message = "Slug tối đa 280 ký tự")
    private String slug;

    private String description;

    @JsonProperty("base_price")
    @NotNull(message = "Giá cơ bản không được để trống")
    private BigDecimal basePrice;

    @JsonProperty("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @JsonProperty("created_by")
    private Long createdBy;
}
