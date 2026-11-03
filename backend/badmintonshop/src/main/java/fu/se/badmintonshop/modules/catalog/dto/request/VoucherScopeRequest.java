package fu.se.badmintonshop.modules.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VoucherScopeRequest {
    @JsonProperty("voucher_id")
    @NotNull(message = "ID voucher không được để trống")
    private Integer voucherId;

    @JsonProperty("scope_type")
    @NotBlank(message = "Loại phạm vi không được để trống")
    private String scopeType;

    @JsonProperty("scope_id")
    @NotNull(message = "ID đối tượng áp dụng không được để trống")
    private Long scopeId;
}
