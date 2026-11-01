package fu.se.badmintonshop.modules.ticket.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusHistoryResponse {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private Long actorId;
    private String note;
    private LocalDateTime createdAt;
}

