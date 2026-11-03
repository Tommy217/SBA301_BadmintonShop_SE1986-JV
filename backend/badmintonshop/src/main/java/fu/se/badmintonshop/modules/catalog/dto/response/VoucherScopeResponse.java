package fu.se.badmintonshop.modules.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VoucherScopeResponse {
    private Integer id;
    @JsonProperty("voucher_id")
    private Integer voucherId;
    @JsonProperty("scope_type")
    private String scopeType;
    @JsonProperty("scope_id")
    private Long scopeId;
}
