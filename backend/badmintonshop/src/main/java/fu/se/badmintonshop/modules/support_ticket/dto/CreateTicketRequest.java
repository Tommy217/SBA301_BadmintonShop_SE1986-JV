package fu.se.badmintonshop.modules.support_ticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    private Long userId; // Can be set from authenticated context

    @NotBlank(message = "ticketType is required (REFUND or EXCHANGE)")
    private String ticketType;

    @NotBlank(message = "reason is required")
    @Size(max = 300, message = "reason cannot exceed 300 characters")
    private String reason;

    @Size(max = 5000, message = "description cannot exceed 5000 characters")
    private String description;

    @NotBlank(message = "refundQrUrl is required for both REFUND and EXCHANGE")
    private String refundQrUrl;

    @NotEmpty(message = "At least one item must be selected for return or exchange")
    @Valid
    private List<TicketItemRequest> items;

    @NotEmpty(message = "At least one evidence file (photo or video) is required")
    private List<String> evidenceUrls;
}

