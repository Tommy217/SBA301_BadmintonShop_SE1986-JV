package fu.se.badmintonshop.modules.inventory.service;

import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;
import fu.se.badmintonshop.modules.catalog.repository.ProductRepository;
import fu.se.badmintonshop.modules.inventory.dto.request.*;
import fu.se.badmintonshop.modules.inventory.dto.response.*;
import fu.se.badmintonshop.modules.inventory.entity.*;
import fu.se.badmintonshop.modules.inventory.exception.InventoryException;
import fu.se.badmintonshop.modules.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {
    private final InventoryVariantRepository variants;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final GoodsReceiptRepository receipts;
    private final GoodsReceiptItemRepository receiptItems;
    private final InventoryMovementRepository movements;
    private final StockAuditRepository audits;
    private final StockAuditItemRepository auditItems;
    private final StockAlertRepository alerts;

    // Reconcile persisted alerts even when catalog changed stock or thresholds.
    @Transactional
    public InventorySnapshotResponse snapshot() {
        var stock = variants.findAllLocked();
        stock.forEach(this::syncAlert);
        return new InventorySnapshotResponse(
                products.findAll(Sort.by("id")).stream().map(InventorySnapshotResponse.ProductRow::from).toList(),
                stock.stream().map(InventorySnapshotResponse.VariantRow::from).toList(),
                suppliers.findAll(Sort.by("id")).stream().map(SupplierResponse::from).toList(),
                listReceipts(), receiptItems.findAll(Sort.by("id")).stream().map(GoodsReceiptItemResponse::from).toList(),
                listMovements(), listAudits(),
                auditItems.findAll(Sort.by("id")).stream().map(StockAuditItemResponse::from).toList(),
                listAlerts(), variants.allocatedQuantity());
    }

    public List<GoodsReceiptResponse> listReceipts() {
        return receipts.findAll(Sort.by("id")).stream().map(GoodsReceiptResponse::from).toList();
    }

    public List<GoodsReceiptItemResponse> receiptLines(Long id) {
        if (!receipts.existsById(id)) throw missing("phiếu nhập");
        return receiptItems.findByReceiptId(id).stream().map(GoodsReceiptItemResponse::from).toList();
    }

    public List<InventoryMovementResponse> listMovements() {
        return movements.findAll(Sort.by("id")).stream().map(InventoryMovementResponse::from).toList();
    }

    public List<StockAuditResponse> listAudits() {
        return audits.findAll(Sort.by("id")).stream().map(StockAuditResponse::from).toList();
    }

    public List<StockAuditItemResponse> auditLines(Long id) {
        if (!audits.existsById(id)) throw missing("phiếu kiểm kê");
        return auditItems.findByAuditIdOrderByVariantId(id).stream().map(StockAuditItemResponse::from).toList();
    }

    public List<StockAlertResponse> listAlerts() {
        return alerts.findAll(Sort.by("id")).stream().map(StockAlertResponse::from).toList();
    }

    @Transactional
    public GoodsReceiptResponse receive(GoodsReceiptRequest request) {
        if (request.supplierId() != null && !suppliers.existsById(request.supplierId())) {
            throw missing("nhà cung cấp");
        }
        Set<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var item : request.items()) {
            if (!seen.add(item.variantId())) throw invalid("Không nhập trùng SKU trong cùng phiếu.");
            total = total.add(item.unitCost().multiply(BigDecimal.valueOf(item.quantity())));
        }
        if (total.compareTo(new BigDecimal("99999999999999")) > 0) {
            throw invalid("Tổng giá trị phiếu nhập vượt giới hạn DECIMAL(14,0).");
        }
        var receipt = new GoodsReceipt();
        receipt.setReceiptCode(code("GR"));
        receipt.setSupplier(request.supplierId() == null ? null : suppliers.getReferenceById(request.supplierId()));
        receipt.setTotalCost(total);
        receipt.setNote(request.note());
        receipt.setReceivedAt(LocalDateTime.now());
        receipts.save(receipt);
        // Lock in ID order for consistent ordering across multi-SKU transactions.
        for (var input : request.items().stream().sorted(Comparator.comparing(GoodsReceiptItemRequest::variantId)).toList()) {
            var variant = lockedVariant(input.variantId());
            int next = Math.addExact(variant.getStockQuantity(), input.quantity());
            var line = new GoodsReceiptItem();
            line.setReceipt(receipt);
            line.setVariant(variant);
            line.setQuantity(input.quantity());
            line.setUnitCost(input.unitCost());
            receiptItems.save(line);
            changeStock(variant, next, "stock_in", "goods_receipt", receipt.getId(), request.note());
        }
        return GoodsReceiptResponse.from(receipt);
    }

    @Transactional
    public InventoryMovementResponse adjust(StockAdjustmentRequest request) {
        var variant = lockedVariant(request.variantId());
        if (Objects.equals(variant.getStockQuantity(), request.countedQty())) {
            throw invalid("Tồn kho mới phải khác số lượng hiện tại.");
        }
        return InventoryMovementResponse.from(changeStock(variant, request.countedQty(),
                "manual_adjust", "manual", null, request.note().trim()));
    }

    @Transactional
    public StockAuditResponse startAudit(StockAuditRequest request) {
        var stock = variants.findAllLocked();
        if (stock.isEmpty()) throw invalid("Chưa có SKU để kiểm kê.");
        var audit = new StockAudit();
        audit.setAuditCode(code("AUD"));
        audit.setStatus("draft");
        audit.setNote(request.note());
        audit.setStartedAt(LocalDateTime.now());
        audits.save(audit);
        for (var variant : stock) {
            var item = new StockAuditItem();
            item.setAudit(audit);
            item.setVariant(variant);
            item.setSystemQty(variant.getStockQuantity());
            item.setCountedQty(variant.getStockQuantity());
            auditItems.save(item);
        }
        return StockAuditResponse.from(audit);
    }

    @Transactional
    public StockAuditResponse updateAudit(Long id, StockAuditUpdateRequest request) {
        var audit = draft(id);
        var lines = new HashMap<Long, StockAuditItem>();
        auditItems.findByAuditIdOrderByVariantId(id).forEach(item -> lines.put(item.getVariant().getId(), item));
        var seen = new HashSet<Long>();
        for (var input : request.items()) {
            if (!seen.add(input.variantId())) throw invalid("SKU kiểm kê bị trùng.");
            var line = lines.get(input.variantId());
            if (line == null) throw invalid("SKU không thuộc phiếu kiểm kê này.");
            line.setCountedQty(input.countedQty());
            line.setNote(input.note());
        }
        return StockAuditResponse.from(audit);
    }

    @Transactional
    public StockAuditResponse completeAudit(Long id) {
        var audit = draft(id);
        for (var line : auditItems.findByAuditIdOrderByVariantId(id)) {
            var variant = lockedVariant(line.getVariant().getId());
            if (!Objects.equals(variant.getStockQuantity(), line.getSystemQty())) {
                throw new InventoryException(409, "Tồn kho SKU " + variant.getSku()
                        + " đã thay đổi. Hãy hủy phiếu và tạo kiểm kê mới.");
            }
            if (!Objects.equals(line.getCountedQty(), line.getSystemQty())) {
                changeStock(variant, line.getCountedQty(), "audit_adjust", "stock_audit", id,
                        line.getNote() == null || line.getNote().isBlank() ? audit.getNote() : line.getNote());
            }
        }
        audit.setStatus("completed");
        audit.setCompletedAt(LocalDateTime.now());
        return StockAuditResponse.from(audit);
    }

    @Transactional
    public StockAuditResponse cancelAudit(Long id) {
        var audit = draft(id);
        audit.setStatus("cancelled");
        return StockAuditResponse.from(audit);
    }

    private StockAudit draft(Long id) {
        var audit = audits.findLockedById(id).orElseThrow(() -> missing("phiếu kiểm kê"));
        if (!"draft".equals(audit.getStatus())) {
            throw new InventoryException(409, "Chỉ thao tác được trên phiếu kiểm kê nháp.");
        }
        return audit;
    }

    private ProductVariant lockedVariant(Long id) {
        return variants.findLockedById(id).orElseThrow(() -> missing("SKU"));
    }

    private InventoryMovement changeStock(ProductVariant variant, int next, String type,
                                           String refType, Long refId, String note) {
        var movement = new InventoryMovement();
        movement.setVariant(variant);
        movement.setMovementType(type);
        movement.setQuantity(next - variant.getStockQuantity());
        movement.setBalanceAfter(next);
        movement.setRefType(refType);
        movement.setRefId(refId);
        movement.setNote(note);
        movement.setCreatedAt(LocalDateTime.now());
        variant.setStockQuantity(next);
        movements.save(movement);
        syncAlert(variant);
        return movement;
    }

    private void syncAlert(ProductVariant variant) {
        var open = alerts.findByVariantIdAndStatus(variant.getId(), "open");
        boolean low = Boolean.TRUE.equals(variant.getIsActive())
                && variant.getStockQuantity() <= variant.getLowStockThreshold();
        if (low && open.isEmpty()) {
            var alert = new StockAlert();
            alert.setVariant(variant);
            alert.setStockAtAlert(variant.getStockQuantity());
            alert.setThreshold(variant.getLowStockThreshold());
            alert.setStatus("open");
            alert.setCreatedAt(LocalDateTime.now());
            alerts.save(alert);
        } else if (!low) {
            open.forEach(alert -> {
                alert.setStatus("resolved");
                alert.setResolvedAt(LocalDateTime.now());
            });
        }
    }

    private static String code(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 25);
    }

    private static InventoryException invalid(String message) { return new InventoryException(400, message); }
    private static InventoryException missing(String name) { return new InventoryException(404, "Không tìm thấy " + name + "."); }
}
