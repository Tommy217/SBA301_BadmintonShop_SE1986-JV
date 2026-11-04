package fu.se.badmintonshop.modules.inventory.dto.response;

import fu.se.badmintonshop.modules.inventory.entity.StockAlert;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockAlertResponse(Long id, Long variantId, Integer stockAtAlert, Integer threshold, String status, LocalDateTime createdAt, LocalDateTime resolvedAt) {
    public static StockAlertResponse from(StockAlert value) {
        return new StockAlertResponse(value.getId(), value.getVariant().getId(), value.getStockAtAlert(), value.getThreshold(), value.getStatus(), value.getCreatedAt(), value.getResolvedAt());
    }
}
