package fu.se.badmintonshop.modules.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductImageRequest {
    @JsonProperty("product_id")
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @JsonProperty("variant_id")
    private Long variantId;

    @NotBlank(message = "URL hình ảnh không được để trống")
    @Size(max = 6990544, message = "Image must be at most 5 MB")
    private String url;

    @JsonProperty("sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
