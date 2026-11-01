package fu.se.badmintonshop.modules.supportticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDecisionRequest {

    @NotBlank(message = "decision is required ('approve' or 'reject')")
    private String decision;

    private String rejectReason;

    private String note;

    private Long managerId;
}

