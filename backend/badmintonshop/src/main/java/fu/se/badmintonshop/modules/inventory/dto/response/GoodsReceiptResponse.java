package fu.se.badmintonshop.modules.inventory.dto.response;

import fu.se.badmintonshop.modules.inventory.entity.GoodsReceipt;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoodsReceiptResponse(Long id, String receiptCode, Integer supplierId, Long receivedBy, BigDecimal totalCost, String note, LocalDateTime receivedAt) {
    public static GoodsReceiptResponse from(GoodsReceipt value) {
        return new GoodsReceiptResponse(value.getId(), value.getReceiptCode(), value.getSupplier() == null ? null : value.getSupplier().getId(), value.getReceivedBy(), value.getTotalCost(), value.getNote(), value.getReceivedAt());
    }
}
