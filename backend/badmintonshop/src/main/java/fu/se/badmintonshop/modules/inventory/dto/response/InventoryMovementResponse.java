package fu.se.badmintonshop.modules.inventory.dto.response;

import fu.se.badmintonshop.modules.inventory.entity.InventoryMovement;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryMovementResponse(Long id, Long variantId, String movementType, Integer quantity, Integer balanceAfter, String refType, Long refId, Long createdBy, String note, LocalDateTime createdAt) {
    public static InventoryMovementResponse from(InventoryMovement value) {
        return new InventoryMovementResponse(value.getId(), value.getVariant().getId(), value.getMovementType(), value.getQuantity(), value.getBalanceAfter(), value.getRefType(), value.getRefId(), value.getCreatedBy(), value.getNote(), value.getCreatedAt());
    }
}
