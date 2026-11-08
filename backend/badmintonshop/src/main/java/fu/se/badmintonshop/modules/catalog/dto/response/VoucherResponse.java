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
public class VoucherResponse {
    private Integer id;
    private String code;

    private String audience;
    private String name;
    private String description;
    @JsonProperty("discount_type")
    private String discountType;
    @JsonProperty("discount_value")
    private BigDecimal discountValue;
    @JsonProperty("min_order")
    private BigDecimal minOrder;
    @JsonProperty("max_discount")
    private BigDecimal maxDiscount;
    @JsonProperty("usage_limit")
    private Integer usageLimit;
    @JsonProperty("per_user_limit")
    private Integer perUserLimit;
    @JsonProperty("used_count")
    private Integer usedCount;
    @JsonProperty("applies_to")
    private String appliesTo;
    @JsonProperty("starts_at")
    private LocalDateTime startsAt;
    @JsonProperty("ends_at")
    private LocalDateTime endsAt;
    @JsonProperty("is_active")
    private Boolean isActive;
    @JsonProperty("created_by")
    private Long createdBy;
}
