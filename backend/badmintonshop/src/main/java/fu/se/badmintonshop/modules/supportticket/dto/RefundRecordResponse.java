package fu.se.badmintonshop.modules.supportticket.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRecordResponse {
    private Long id;
    private Long ticketId;
    private Long orderId;
    private BigDecimal amount;
    private String bankReference;
    private Long transferredBy;
    private String note;
    private LocalDateTime transferredAt;
}

