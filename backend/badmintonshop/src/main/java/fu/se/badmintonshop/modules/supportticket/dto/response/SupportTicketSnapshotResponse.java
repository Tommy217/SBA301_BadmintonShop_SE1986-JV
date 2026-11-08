package fu.se.badmintonshop.modules.supportticket.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketSnapshotResponse {
    @JsonProperty("id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonProperty("ticket_code")
    private String ticketCode;

    @JsonProperty("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    @JsonProperty("order_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    @JsonProperty("ticket_type")
    private String ticketType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("description")
    private String description;

    @JsonProperty("cause")
    private String cause;

    @JsonProperty("reject_reason")
    private String rejectReason;

    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;

    @JsonProperty("refund_qr_url")
    private String refundQrUrl;

    @JsonProperty("reviewed_by")
    private Long reviewedBy;

    @JsonProperty("reviewed_at")
    private LocalDateTime reviewedAt;

    @JsonProperty("approved_by")
    private Long approvedBy;

    @JsonProperty("approved_at")
    private LocalDateTime approvedAt;

    @JsonProperty("resolved_at")
    private LocalDateTime resolvedAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("evidence_urls")
    private List<String> evidenceUrls;

    @JsonProperty("inspection")
    private TicketInspectionSnapshotResponse inspection;

    @JsonProperty("refund_record")
    private RefundRecordSnapshotResponse refundRecord;
}
