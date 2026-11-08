package fu.se.badmintonshop.modules.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherUsageSnapshotResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("voucher_id")
    private Integer voucherId;

    @JsonProperty("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    @JsonProperty("order_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    @JsonProperty("discount_amount")
    private BigDecimal discountAmount;

    @JsonProperty("used_at")
    private LocalDateTime usedAt;
}
