package fu.se.badmintonshop.modules.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("order_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    @JsonProperty("from_status")
    private String fromStatus;

    @JsonProperty("to_status")
    private String toStatus;

    @JsonProperty("changed_by")
    private Long changedBy;

    @JsonProperty("note")
    private String note;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
