package fu.se.badmintonshop.modules.inventory.controller;

import fu.se.badmintonshop.modules.inventory.dto.request.*;
import fu.se.badmintonshop.modules.inventory.dto.response.*;
import fu.se.badmintonshop.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService service;

    @GetMapping
    public InventorySnapshotResponse snapshot() { return service.snapshot(); }

    @GetMapping("/receipts")
    public List<GoodsReceiptResponse> receipts() { return service.listReceipts(); }

    @GetMapping("/receipts/{id}/items")
    public List<GoodsReceiptItemResponse> receiptItems(@PathVariable Long id) { return service.receiptLines(id); }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public GoodsReceiptResponse receive(@Valid @RequestBody GoodsReceiptRequest request) { return service.receive(request); }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementResponse adjust(@Valid @RequestBody StockAdjustmentRequest request) { return service.adjust(request); }

    @GetMapping("/movements")
    public List<InventoryMovementResponse> movements() { return service.listMovements(); }

    @GetMapping("/audits")
    public List<StockAuditResponse> audits() { return service.listAudits(); }

    @GetMapping("/audits/{id}/items")
    public List<StockAuditItemResponse> auditItems(@PathVariable Long id) { return service.auditLines(id); }

    @PostMapping("/audits")
    @ResponseStatus(HttpStatus.CREATED)
    public StockAuditResponse start(@Valid @RequestBody StockAuditRequest request) { return service.startAudit(request); }

    @PutMapping("/audits/{id}")
    public StockAuditResponse update(@PathVariable Long id, @Valid @RequestBody StockAuditUpdateRequest request) {
        return service.updateAudit(id, request);
    }

    @PostMapping("/audits/{id}/complete")
    public StockAuditResponse complete(@PathVariable Long id) { return service.completeAudit(id); }

    @PostMapping("/audits/{id}/cancel")
    public StockAuditResponse cancel(@PathVariable Long id) { return service.cancelAudit(id); }

    @GetMapping("/alerts")
    public List<StockAlertResponse> alerts() { return service.listAlerts(); }
}
