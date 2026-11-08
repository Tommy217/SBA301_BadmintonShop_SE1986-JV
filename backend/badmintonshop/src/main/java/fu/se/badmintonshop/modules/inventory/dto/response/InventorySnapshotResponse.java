package fu.se.badmintonshop.modules.inventory.dto.response;

import fu.se.badmintonshop.modules.catalog.entity.Product;
import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.List;

public record InventorySnapshotResponse(
        List<ProductRow> products,
        List<VariantRow> productVariants,
        List<SupplierResponse> suppliers,
        List<GoodsReceiptResponse> goodsReceipts,
        List<GoodsReceiptItemResponse> goodsReceiptItems,
        List<InventoryMovementResponse> inventoryMovements,
        List<StockAuditResponse> stockAudits,
        List<StockAuditItemResponse> stockAuditItems,
        List<StockAlertResponse> stockAlerts,
        Long allocatedQuantity) {
    public record ProductRow(
            Long id, String name, String productType, String brand, Boolean isActive) {
        public static ProductRow from(Product p) {
            return new ProductRow(
                    p.getId(),
                    p.getName(),
                    p.getProductType(),
                    p.getBrand().getName(),
                    p.getIsActive());
        }
    }

    public record VariantRow(
            Long id,
            Long productId,
            String sku,
            String color,
            String size,
            Integer stockQuantity,
            Integer lowStockThreshold,
            BigDecimal price,
            BigDecimal costPrice,
            Boolean isActive) {
        public static VariantRow from(ProductVariant v) {
            return new VariantRow(
                    v.getId(),
                    v.getProductId(),
                    v.getSku(),
                    v.getColor(),
                    v.getSize(),
                    v.getStockQuantity(),
                    v.getLowStockThreshold(),
                    v.getPrice(),
                    v.getCostPrice(),
                    v.getIsActive());
        }
    }
}
