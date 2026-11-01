package fu.se.badmintonshop.modules.supportticket.dto;

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

