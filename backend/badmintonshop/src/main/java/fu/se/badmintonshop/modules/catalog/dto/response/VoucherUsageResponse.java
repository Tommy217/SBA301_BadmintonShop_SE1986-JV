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
public class VoucherUsageResponse {
    private Long id;
    @JsonProperty("voucher_id")
    private Integer voucherId;
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("order_id")
    private Long orderId;
    @JsonProperty("discount_amount")
    private BigDecimal discountAmount;
    @JsonProperty("used_at")
    private LocalDateTime usedAt;
}
