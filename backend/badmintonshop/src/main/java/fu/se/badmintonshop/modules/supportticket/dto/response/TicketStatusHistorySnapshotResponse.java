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
public class TicketStatusHistorySnapshotResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("ticket_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ticketId;

    @JsonProperty("from_status")
    private String fromStatus;

    @JsonProperty("to_status")
    private String toStatus;

    @JsonProperty("actor_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long actorId;

    @JsonProperty("note")
    private String note;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
