package fu.se.badmintonshop.modules.invoice;

import fu.se.badmintonshop.modules.catalog.entity.*;
import fu.se.badmintonshop.modules.catalog.repository.*;
import fu.se.badmintonshop.modules.invoice.dto.request.*;
import fu.se.badmintonshop.modules.invoice.exception.InvoiceException;
import fu.se.badmintonshop.modules.invoice.repository.*;
import fu.se.badmintonshop.modules.invoice.service.InvoiceService;
import fu.se.badmintonshop.modules.order.entity.*;
import fu.se.badmintonshop.modules.order.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoiceIntegrationTest {
    @Autowired InvoiceService service;
    @Autowired InvoiceRepository invoices;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired TransactionTemplate transaction;
    @Autowired MockMvc mvc;
    Long orderId;

    @BeforeEach
    void seed() {
        orderId = transaction.execute(status -> {
            String key = UUID.randomUUID().toString();
            var brand = brands.save(Brand.builder().name(key).slug(key).build());
            var category = categories.save(Category.builder().name(key).slug(key).build());
            var product = products.save(Product.builder().brand(brand).category(category).name("Vợt cầu lông")
                    .slug(key).productType("racket").basePrice(new BigDecimal("100000")).build());
            var variant = variants.save(ProductVariant.builder().productId(product.getId()).product(product)
                    .sku(key).price(new BigDecimal("100000")).costPrice(new BigDecimal("50000"))
                    .stockQuantity(10).lowStockThreshold(5).build());
            var order = orders.save(Order.builder().orderCode(key.substring(0, 30)).status("completed")
                    .paymentStatus("paid").receiverName("Nguyễn Văn An").shippingAddress("Hà Nội")
                    .subtotal(new BigDecimal("200000")).discountAmount(new BigDecimal("10000"))
                    .shippingFee(new BigDecimal("30000")).totalAmount(new BigDecimal("220000")).build());
            orderItems.save(OrderItem.builder().orderId(order.getId()).productId(product.getId())
                    .variantId(variant.getId()).productName("Vợt cầu lông")
                    .unitPrice(new BigDecimal("100000")).quantity(2).lineTotal(new BigDecimal("200000")).build());
            return order.getId();
        });
    }

    InvoiceRequest request() {
        var request = new InvoiceRequest();
        request.setOrderId(orderId);
        request.setBuyerEmail("buyer@example.com");
        return request;
    }

    @Test
    void issuesDatabaseSnapshotAndRejectsDuplicateOrUnpaidOrder() {
        var invoice = service.create(request());
        assertEquals("Nguyễn Văn An", invoice.buyerName());
        assertEquals(new BigDecimal("220000"), invoice.totalAmount());
        assertEquals(1, invoice.items().size());
        assertEquals("Vợt cầu lông", invoice.items().getFirst().description());
        assertNotNull(invoice.issuedAt());
        assertEquals(30, invoice.invoiceNo().length());
        assertThrows(InvoiceException.class, () -> service.create(request()));
        transaction.executeWithoutResult(s -> orders.findById(orderId).orElseThrow().setPaymentStatus("unpaid"));
        assertThrows(InvoiceException.class, () -> service.create(request()));
    }

    @Test
    void adjustmentReplacementAndCancellationPersistAndEnforceLifecycle() {
        var original = service.create(request());
        var adjustment = new InvoiceAdjustmentRequest();
        adjustment.setAmount(new BigDecimal("-15000"));
        adjustment.setReason("Điều chỉnh giảm");
        var adjusted = service.adjust(original.id(), adjustment);
        assertEquals(original.id(), adjusted.parentInvoiceId());
        assertEquals(new BigDecimal("-15000"), adjusted.items().getFirst().lineTotal());
        assertEquals(BigDecimal.ZERO, adjusted.shippingFee());
        assertThrows(InvoiceException.class, () -> service.replace(adjusted.id()));
        adjustment.setAmount(BigDecimal.ZERO);
        long count = invoices.count();
        assertThrows(InvoiceException.class, () -> service.adjust(original.id(), adjustment));
        assertEquals(count, invoices.count());
        var replacement = service.replace(original.id());
        assertEquals("replaced", service.get(original.id()).status());
        assertEquals(original.id(), replacement.parentInvoiceId());
        assertEquals(original.items().getFirst().lineTotal(), replacement.items().getFirst().lineTotal());
        assertThrows(InvoiceException.class, () -> service.replace(original.id()));
        service.cancel(replacement.id());
        assertEquals("cancelled", service.get(replacement.id()).status());
        assertThrows(InvoiceException.class, () -> service.cancel(replacement.id()));
    }

    @Test
    void deliveryPersistsQueueAndDownloadHistory() {
        var invoice = service.create(request());
        var request = new InvoiceDeliveryRequest();
        request.setChannel("email");
        assertThrows(InvoiceException.class, () -> service.deliver(invoice.id(), request));
        request.setRecipient("buyer@example.com");
        assertEquals("queued", service.deliver(invoice.id(), request).status());
        request.setChannel("download");
        var download = service.deliver(invoice.id(), request);
        assertEquals("sent", download.status());
        assertNotNull(download.sentAt());
        assertEquals(2, service.get(invoice.id()).deliveries().size());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "STAFF")
    void httpContractIncludesNestedDataAndRejectsInvalidRequests() throws Exception {
        var invoice = service.create(request());
        mvc.perform(get("/api/invoices/" + invoice.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.items[0].unitPrice").value(100000))
                .andExpect(jsonPath("$.buyerName").value("Nguyễn Văn An"));
        mvc.perform(get("/api/invoices")).andExpect(status().isOk()).andExpect(jsonPath("$[0].orderCode").isString());
        mvc.perform(post("/api/invoices/" + invoice.id() + "/adjustments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1.5,\"reason\":\"test\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        mvc.perform(post("/api/invoices/" + invoice.id() + "/deliveries").contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"email\",\"recipient\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/invoices/9223372036854775807")).andExpect(status().isNotFound());
    }
}
