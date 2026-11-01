package fu.se.badmintonshop.modules.supportticket.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private String ticketCode;
    private Long userId;
    private Long orderId;
    private String orderCode;
    private String ticketType;
    private String status;
    private String reason;
    private String description;
    private String cause;
    private String rejectReason;
    private BigDecimal refundAmount;
    private String refundQrUrl;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TicketItemResponse> items;
    private List<String> evidenceUrls;
    private TicketInspectionResponse inspection;
    private RefundRecordResponse refundRecord;
    private List<TicketStatusHistoryResponse> statusHistories;
}

