package fu.se.badmintonshop.modules.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundTransferRequest {

    @NotBlank(message = "bankReference is required")
    private String bankReference;

    private String note;

    private Long staffId;
}

