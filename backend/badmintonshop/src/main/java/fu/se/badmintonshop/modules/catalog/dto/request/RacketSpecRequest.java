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
public class RacketSpecRequest {
    @JsonProperty("product_id")
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @JsonProperty("balance_type")
    @NotBlank(message = "Điểm cân bằng không được để trống")
    private String balanceType;

    @NotBlank(message = "Độ cứng đũa không được để trống")
    private String flex;

    @JsonProperty("play_style")
    @NotBlank(message = "Lối chơi không được để trống")
    private String playStyle;

    @JsonProperty("max_tension_lbs")
    private BigDecimal maxTensionLbs;

    @Size(max = 150, message = "Chất liệu tối đa 150 ký tự")
    private String material;

    @JsonProperty("shaft_diameter")
    private BigDecimal shaftDiameter;
}
