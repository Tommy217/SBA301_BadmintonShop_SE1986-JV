package fu.se.badmintonshop.modules.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

// body cua POST /api/manager/shipments/{id}/cancel
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelShipmentRequest {

    @NotBlank(message = "Vui lòng nhập lý do huỷ.")
    @Size(max = 300, message = "Lý do huỷ tối đa 300 ký tự.")
    private String reason;
}
