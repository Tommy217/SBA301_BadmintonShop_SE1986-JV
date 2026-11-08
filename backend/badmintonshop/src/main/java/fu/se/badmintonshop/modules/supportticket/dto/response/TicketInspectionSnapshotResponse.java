package fu.se.badmintonshop.modules.supportticket.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketInspectionSnapshotResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("ticket_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ticketId;

    @JsonProperty("staff_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long staffId;

    @JsonProperty("result")
    private String result;

    @JsonProperty("note")
    private String note;

    @JsonProperty("inspected_at")
    private LocalDateTime inspectedAt;
}
