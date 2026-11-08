package fu.se.badmintonshop.modules.supportticket.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketItemSnapshotResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("ticket_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ticketId;

    @JsonProperty("order_item_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderItemId;

    @JsonProperty("quantity")
    private Integer quantity;
}
