package fu.se.badmintonshop.modules.inventory;

import fu.se.badmintonshop.modules.catalog.entity.*;
import fu.se.badmintonshop.modules.catalog.repository.*;
import fu.se.badmintonshop.modules.inventory.dto.request.*;
import fu.se.badmintonshop.modules.inventory.exception.InventoryException;
import fu.se.badmintonshop.modules.inventory.repository.*;
import fu.se.badmintonshop.modules.inventory.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryIntegrationTest {
    @Autowired InventoryService service;
    @Autowired SupplierService supplierService;
    @Autowired ProductVariantRepository variants;
    @Autowired ProductRepository products;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired GoodsReceiptRepository receipts;
    @Autowired GoodsReceiptItemRepository receiptItems;
    @Autowired InventoryMovementRepository movements;
    @Autowired StockAuditRepository audits;
    @Autowired StockAlertRepository alerts;
    @Autowired TransactionTemplate transaction;
    @Autowired MockMvc mvc;
    Long variantId;

    @BeforeEach
    void seed() {
        variantId = transaction.execute(status -> {
            String key = UUID.randomUUID().toString();
            var brand = brands.save(Brand.builder().name(key).slug(key).build());
            var category = categories.save(Category.builder().name(key).slug(key).build());
            var product = products.save(Product.builder().brand(brand).category(category).name("Test racket")
                    .slug(key).productType("racket").basePrice(new BigDecimal("100000")).build());
            return variants.save(ProductVariant.builder().productId(product.getId()).product(product)
                    .sku(key).price(new BigDecimal("100000")).costPrice(new BigDecimal("50000"))
                    .stockQuantity(10).lowStockThreshold(5).build()).getId();
        });
    }

    GoodsReceiptRequest receipt(Long id, int quantity) {
        return new GoodsReceiptRequest(null, "Nhập kho", List.of(
                new GoodsReceiptItemRequest(id, quantity, new BigDecimal("75000"))));
    }

    @Test
    void receiptPersistsLinesBalanceAndLedger() {
        var supplier = supplierService.create(new SupplierRequest("Supplier", null, null, null));
        var result = service.receive(new GoodsReceiptRequest(supplier.id(), "Delivery", receipt(variantId, 4).items()));
        assertEquals(new BigDecimal("300000"), result.totalCost());
        assertEquals(supplier.id(), result.supplierId());
        assertEquals(14, variants.findById(variantId).orElseThrow().getStockQuantity());
        var line = service.receiptLines(result.id()).getFirst();
        assertEquals(variantId, line.variantId());
        assertEquals(result.id(), line.receiptId());
        var movement = service.listMovements().stream().filter(m -> result.id().equals(m.refId())
                && "goods_receipt".equals(m.refType())).findFirst().orElseThrow();
        assertEquals(4, movement.quantity());
        assertEquals(14, movement.balanceAfter());
        assertThrows(InventoryException.class, () -> supplierService.delete(supplier.id()));
    }

    @Test
    void invalidLaterLineRollsBackWholeReceipt() {
        long receiptCount = receipts.count(), lineCount = receiptItems.count(), movementCount = movements.count();
        var request = new GoodsReceiptRequest(null, null, List.of(
                new GoodsReceiptItemRequest(variantId, 3, BigDecimal.TEN),
                new GoodsReceiptItemRequest(Long.MAX_VALUE, 1, BigDecimal.TEN)));
        assertThrows(InventoryException.class, () -> service.receive(request));
        assertEquals(10, variants.findById(variantId).orElseThrow().getStockQuantity());
        assertEquals(receiptCount, receipts.count());
        assertEquals(lineCount, receiptItems.count());
        assertEquals(movementCount, movements.count());
    }

    @Test
    void duplicateAndOverflowReceiptsAreRejectedWithoutChanges() {
        var item = new GoodsReceiptItemRequest(variantId, 1, BigDecimal.ONE);
        assertThrows(InventoryException.class, () -> service.receive(
                new GoodsReceiptRequest(null, null, List.of(item, item))));
        assertThrows(ArithmeticException.class, () -> service.receive(new GoodsReceiptRequest(null, null,
                List.of(new GoodsReceiptItemRequest(variantId, Integer.MAX_VALUE, BigDecimal.ONE)))));
        assertEquals(10, variants.findById(variantId).orElseThrow().getStockQuantity());
    }

    @Test
    void completedAuditUpdatesOnceAndComputedDifferenceIsReadable() {
        var audit = service.startAudit(new StockAuditRequest("Count"));
        service.updateAudit(audit.id(), new StockAuditUpdateRequest(List.of(
                new StockAuditItemRequest(variantId, 7, "Counted"))));
        assertEquals(-3, service.auditLines(audit.id()).stream()
                .filter(i -> i.variantId().equals(variantId)).findFirst().orElseThrow().diffQty());
        assertEquals("completed", service.completeAudit(audit.id()).status());
        assertEquals(7, variants.findById(variantId).orElseThrow().getStockQuantity());
        long count = movements.count();
        assertThrows(InventoryException.class, () -> service.completeAudit(audit.id()));
        assertEquals(count, movements.count());
    }

    @Test
    void staleAuditDoesNotOverwriteNewStock() {
        var audit = service.startAudit(new StockAuditRequest(null));
        service.updateAudit(audit.id(), new StockAuditUpdateRequest(List.of(
                new StockAuditItemRequest(variantId, 4, null))));
        service.receive(receipt(variantId, 2));
        var error = assertThrows(InventoryException.class, () -> service.completeAudit(audit.id()));
        assertEquals(409, error.getStatus());
        assertEquals(12, variants.findById(variantId).orElseThrow().getStockQuantity());
        assertEquals("draft", audits.findById(audit.id()).orElseThrow().getStatus());
        assertEquals("cancelled", service.cancelAudit(audit.id()).status());
        assertThrows(InventoryException.class, () -> service.updateAudit(audit.id(),
                new StockAuditUpdateRequest(List.of(new StockAuditItemRequest(variantId, 4, null)))));
    }

    @Test
    void alertsOpenOnceResolveAndReopen() {
        service.adjust(new StockAdjustmentRequest(variantId, 3, "Count"));
        service.snapshot();
        service.snapshot();
        assertEquals(1, alerts.findByVariantIdAndStatus(variantId, "open").size());
        service.receive(receipt(variantId, 5));
        assertEquals(0, alerts.findByVariantIdAndStatus(variantId, "open").size());
        assertNotNull(alerts.findByVariantIdAndStatus(variantId, "resolved").getFirst().getResolvedAt());
        service.adjust(new StockAdjustmentRequest(variantId, 0, "Damaged"));
        assertEquals(1, alerts.findByVariantIdAndStatus(variantId, "open").size());
    }

    @Test
    void httpContractAndValidation() throws Exception {
        mvc.perform(get("/api/inventory")).andExpect(status().isOk())
                .andExpect(jsonPath("$.productVariants").isArray())
                .andExpect(jsonPath("$.goodsReceiptItems").isArray())
                .andExpect(jsonPath("$.stockAuditItems").isArray())
                .andExpect(jsonPath("$.allocatedQuantity").isNumber());
        mvc.perform(post("/api/inventory/receipts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":" + variantId + ",\"quantity\":0,\"unitCost\":10}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        mvc.perform(post("/api/inventory/adjustments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variantId + ",\"countedQty\":-1,\"note\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/inventory/receipts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":" + variantId + ",\"quantity\":2,\"unitCost\":1200}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalCost").value(2400));
        mvc.perform(get("/api/inventory/receipts/9223372036854775807/items"))
                .andExpect(status().isNotFound());
    }
}
