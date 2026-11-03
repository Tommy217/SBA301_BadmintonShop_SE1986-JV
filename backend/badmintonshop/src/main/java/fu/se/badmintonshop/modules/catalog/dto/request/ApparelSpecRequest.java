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
public class ApparelSpecRequest {
    @JsonProperty("product_id")
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotBlank(message = "Giới tính không được để trống")
    private String gender;

    @JsonProperty("apparel_kind")
    @NotBlank(message = "Loại trang phục không được để trống")
    private String apparelKind;

    @Size(max = 150, message = "Chất liệu tối đa 150 ký tự")
    private String material;
}
