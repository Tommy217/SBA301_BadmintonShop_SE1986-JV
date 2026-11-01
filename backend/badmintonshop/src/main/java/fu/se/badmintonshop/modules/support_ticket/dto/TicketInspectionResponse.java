package fu.se.badmintonshop.modules.support_ticket.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketInspectionResponse {
    private Long id;
    private Long staffId;
    private String result;
    private String note;
    private LocalDateTime inspectedAt;
}

