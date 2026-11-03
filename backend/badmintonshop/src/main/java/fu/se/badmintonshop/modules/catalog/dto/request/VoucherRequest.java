package fu.se.badmintonshop.modules.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fu.se.badmintonshop.modules.catalog.dto.IsoLocalDateTimeDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 40, message = "Mã voucher tối đa 40 ký tự")
    private String code;

    @NotBlank(message = "Tên voucher không được để trống")
    @Size(max = 150, message = "Tên voucher tối đa 150 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @JsonProperty("discount_type")
    @NotBlank(message = "Loại giảm giá không được để trống")
    private String discountType;

    @JsonProperty("discount_value")
    @NotNull(message = "Giá trị giảm không được để trống")
    private BigDecimal discountValue;

    @JsonProperty("min_order")
    @Builder.Default
    private BigDecimal minOrder = BigDecimal.ZERO;

    @JsonProperty("max_discount")
    private BigDecimal maxDiscount;

    @JsonProperty("usage_limit")
    private Integer usageLimit;

    @JsonProperty("per_user_limit")
    @Builder.Default
    private Integer perUserLimit = 1;

    @JsonProperty("applies_to")
    @Builder.Default
    private String appliesTo = "all";

    @JsonProperty("starts_at")
    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @JsonDeserialize(using = IsoLocalDateTimeDeserializer.class)
    private LocalDateTime startsAt;

    @JsonProperty("ends_at")
    @NotNull(message = "Thời gian kết thúc không được để trống")
    @JsonDeserialize(using = IsoLocalDateTimeDeserializer.class)
    private LocalDateTime endsAt;

    @JsonProperty("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @JsonProperty("created_by")
    private Long createdBy;
}
