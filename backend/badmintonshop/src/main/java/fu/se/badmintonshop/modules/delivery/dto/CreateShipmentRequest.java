package fu.se.badmintonshop.modules.delivery.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShipmentRequest {

    @NotNull(message = "Vui lòng chọn đơn hàng.")
    private Long orderId;

    @NotBlank(message = "Vui lòng chọn đơn vị vận chuyển.")
    @Size(max = 30, message = "Mã đơn vị vận chuyển không hợp lệ.")
    private String carrierCode;

    // standard | fast | express, de trong = standard
    @Pattern(regexp = "standard|fast|express", message = "Gói dịch vụ chỉ nhận standard, fast hoặc express.")
    private String serviceType;

    @Positive(message = "Cân nặng phải lớn hơn 0.")
    @Max(value = 50000, message = "Cân nặng tối đa 50.000 gram.")
    private Integer weightGrams;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
    private String note;
}
