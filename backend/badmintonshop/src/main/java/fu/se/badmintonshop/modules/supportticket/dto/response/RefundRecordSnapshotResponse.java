package fu.se.badmintonshop.modules.supportticket.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRecordSnapshotResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("ticket_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ticketId;

    @JsonProperty("payment_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long paymentId;

    @JsonProperty("order_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("bank_reference")
    private String bankReference;

    @JsonProperty("transferred_by")
    private Long transferredBy;

    @JsonProperty("note")
    private String note;

    @JsonProperty("transferred_at")
    private LocalDateTime transferredAt;
}
