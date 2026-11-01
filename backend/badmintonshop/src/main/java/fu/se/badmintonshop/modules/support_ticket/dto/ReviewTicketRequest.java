package fu.se.badmintonshop.modules.support_ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTicketRequest {

    @NotBlank(message = "action must be 'approve' or 'reject'")
    private String action;

    private String cause; // Required when action is 'approve': STORE_FAULT or CUSTOMER_PREFERENCE

    private String rejectReason; // Required when action is 'reject'

    private Long staffId;

    private String note;
}

