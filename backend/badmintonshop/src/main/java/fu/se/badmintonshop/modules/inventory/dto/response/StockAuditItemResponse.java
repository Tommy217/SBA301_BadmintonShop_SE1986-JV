package fu.se.badmintonshop.modules.inventory.dto.response;

import fu.se.badmintonshop.modules.inventory.entity.StockAuditItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockAuditItemResponse(Long id, Long auditId, Long variantId, Integer systemQty, Integer countedQty, Integer diffQty, String note) {
    public static StockAuditItemResponse from(StockAuditItem value) {
        return new StockAuditItemResponse(value.getId(), value.getAudit().getId(), value.getVariant().getId(), value.getSystemQty(), value.getCountedQty(), value.getCountedQty() - value.getSystemQty(), value.getNote());
    }
}
