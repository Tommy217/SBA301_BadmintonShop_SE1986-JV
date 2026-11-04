package fu.se.badmintonshop.modules.inventory.dto.response;

import fu.se.badmintonshop.modules.inventory.entity.GoodsReceiptItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoodsReceiptItemResponse(Long id, Long receiptId, Long variantId, Integer quantity, BigDecimal unitCost) {
    public static GoodsReceiptItemResponse from(GoodsReceiptItem value) {
        return new GoodsReceiptItemResponse(value.getId(), value.getReceipt().getId(), value.getVariant().getId(), value.getQuantity(), value.getUnitCost());
    }
}
