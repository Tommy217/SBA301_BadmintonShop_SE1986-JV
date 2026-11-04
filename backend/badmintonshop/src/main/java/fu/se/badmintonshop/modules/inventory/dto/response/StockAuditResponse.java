package fu.se.badmintonshop.modules.inventory.dto.response;

import fu.se.badmintonshop.modules.inventory.entity.StockAudit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockAuditResponse(Long id, String auditCode, String status, Long conductedBy, String note, LocalDateTime startedAt, LocalDateTime completedAt) {
    public static StockAuditResponse from(StockAudit value) {
        return new StockAuditResponse(value.getId(), value.getAuditCode(), value.getStatus(), value.getConductedBy(), value.getNote(), value.getStartedAt(), value.getCompletedAt());
    }
}
