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
public class ShoeSpecRequest {
    @JsonProperty("product_id")
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotBlank(message = "Giới tính không được để trống")
    private String gender;

    @JsonProperty("sole_type")
    @Size(max = 100, message = "Loại đế tối đa 100 ký tự")
    private String soleType;

    @Size(max = 200, message = "Công nghệ tối đa 200 ký tự")
    private String technology;

    @JsonProperty("foot_width")
    @Builder.Default
    private String footWidth = "regular";
}
