package fu.se.badmintonshop.modules.invoice.service;

import fu.se.badmintonshop.modules.invoice.dto.request.*;
import fu.se.badmintonshop.modules.invoice.dto.response.*;
import fu.se.badmintonshop.modules.invoice.entity.*;
import fu.se.badmintonshop.modules.invoice.exception.InvoiceException;
import fu.se.badmintonshop.modules.invoice.repository.*;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.repository.OrderItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {
    private final InvoiceRepository invoices;
    private final InvoiceItemRepository items;
    private final InvoiceDeliveryRepository deliveries;
    private final OrderItemRepository orderItems;
    private final EntityManager entityManager;

    public List<InvoiceResponse> list() {
        var all = invoices.findAllByOrderByIssuedAtDescIdDesc();
        var itemGroups = items.findAll().stream().collect(Collectors.groupingBy(i -> i.getInvoice().getId()));
        var deliveryGroups = deliveries.findAll().stream().collect(Collectors.groupingBy(d -> d.getInvoice().getId()));
        return all.stream().map(i -> response(i, itemGroups.getOrDefault(i.getId(), List.of()),
                deliveryGroups.getOrDefault(i.getId(), List.of()))).toList();
    }

    public InvoiceResponse get(Long id) {
        return response(invoices.findById(id).orElseThrow(() -> new InvoiceException(404, "Không tìm thấy hóa đơn.")));
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        // Serialize issuance for the same order without adding columns to the supplied schema.
        Order order = entityManager.find(Order.class, request.getOrderId(), LockModeType.PESSIMISTIC_WRITE);
        if (order == null) throw new InvoiceException(404, "Không tìm thấy đơn hàng.");
        if (!"completed".equals(order.getStatus()) || !"paid".equals(order.getPaymentStatus()))
            throw new InvoiceException(409, "Chỉ phát hành hóa đơn cho đơn hoàn tất và đã thanh toán.");
        if (invoices.existsByOrderIdAndInvoiceType(order.getId(), "sale"))
            throw new InvoiceException(409, "Đơn hàng đã có hóa đơn. Hãy dùng thao tác thay thế nếu cần.");
        var sourceItems = orderItems.findByOrderId(order.getId());
        if (sourceItems.isEmpty()) throw new InvoiceException(409, "Đơn hàng không có sản phẩm.");
        Invoice invoice = Invoice.builder().invoiceNo(number()).order(order)
                .buyerName(valueOr(request.getBuyerName(), order.getReceiverName()))
                .buyerEmail(request.getBuyerEmail())
                .buyerAddress(valueOr(request.getBuyerAddress(), order.getShippingAddress()))
                .buyerTaxCode(request.getBuyerTaxCode()).subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount()).shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount()).build();
        invoices.saveAndFlush(invoice);
        for (var source : sourceItems) {
            items.save(InvoiceItem.builder().invoice(invoice).description(source.getProductName())
                    .unitPrice(source.getUnitPrice()).quantity(source.getQuantity()).lineTotal(source.getLineTotal()).build());
        }
        return response(invoice);
    }

    @Transactional
    public InvoiceResponse adjust(Long id, InvoiceAdjustmentRequest request) {
        Invoice original = issued(id, true);
        if (request.getAmount().signum() == 0) throw new InvoiceException(400, "Số tiền điều chỉnh phải khác 0.");
        Invoice adjustment = copy(original);
        adjustment.setInvoiceType("adjustment");
        adjustment.setReason(request.getReason().trim());
        adjustment.setSubtotal(request.getAmount());
        adjustment.setTotalAmount(request.getAmount());
        adjustment.setDiscountAmount(BigDecimal.ZERO);
        adjustment.setShippingFee(BigDecimal.ZERO);
        invoices.saveAndFlush(adjustment);
        items.save(InvoiceItem.builder().invoice(adjustment)
                .description(valueOr(request.getDescription(), "Điều chỉnh hóa đơn " + original.getInvoiceNo()))
                .quantity(1).unitPrice(request.getAmount()).lineTotal(request.getAmount()).build());
        return response(adjustment);
    }

    @Transactional
    public InvoiceResponse replace(Long id) {
        Invoice original = issued(id, true);
        Invoice replacement = copy(original);
        invoices.saveAndFlush(replacement);
        for (InvoiceItem item : items.findByInvoiceIdOrderByIdAsc(id)) {
            items.save(InvoiceItem.builder().invoice(replacement).description(item.getDescription())
                    .unitPrice(item.getUnitPrice()).quantity(item.getQuantity()).lineTotal(item.getLineTotal()).build());
        }
        original.setStatus("replaced");
        return response(replacement);
    }

    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice invoice = issued(id, false);
        invoice.setStatus("cancelled");
        return response(invoice);
    }

    @Transactional
    public InvoiceDeliveryResponse deliver(Long id, InvoiceDeliveryRequest request) {
        Invoice invoice = issued(id, false);
        boolean email = "email".equals(request.getChannel());
        if (email && (request.getRecipient() == null || request.getRecipient().isBlank()))
            throw new InvoiceException(400, "Vui lòng nhập email người nhận.");
        // Email is queued for a delivery worker; do not claim that SMTP delivery succeeded.
        InvoiceDelivery delivery = deliveries.save(InvoiceDelivery.builder().invoice(invoice)
                .channel(request.getChannel()).recipient(email ? request.getRecipient().trim() : null)
                .status(email ? "queued" : "sent").sentAt(email ? null : LocalDateTime.now()).build());
        return deliveryResponse(delivery);
    }

    private Invoice issued(Long id, boolean saleOnly) {
        Invoice invoice = invoices.findForUpdate(id)
                .orElseThrow(() -> new InvoiceException(404, "Không tìm thấy hóa đơn."));
        if (!"issued".equals(invoice.getStatus()))
            throw new InvoiceException(409, "Hóa đơn đã hủy hoặc đã được thay thế.");
        if (saleOnly && !"sale".equals(invoice.getInvoiceType()))
            throw new InvoiceException(409, "Thao tác này chỉ áp dụng cho hóa đơn bán.");
        return invoice;
    }

    private Invoice copy(Invoice original) {
        return Invoice.builder().invoiceNo(number()).order(original.getOrder()).parentInvoice(original)
                .buyerName(original.getBuyerName()).buyerEmail(original.getBuyerEmail())
                .buyerAddress(original.getBuyerAddress()).buyerTaxCode(original.getBuyerTaxCode())
                .subtotal(original.getSubtotal()).discountAmount(original.getDiscountAmount())
                .shippingFee(original.getShippingFee()).totalAmount(original.getTotalAmount()).build();
    }

    private String number() { return "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase(Locale.ROOT); }
    private String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    private InvoiceResponse response(Invoice invoice) {
        return response(invoice, items.findByInvoiceIdOrderByIdAsc(invoice.getId()),
                deliveries.findByInvoiceIdOrderByIdDesc(invoice.getId()));
    }

    private InvoiceResponse response(Invoice i, List<InvoiceItem> invoiceItems, List<InvoiceDelivery> invoiceDeliveries) {
        return new InvoiceResponse(i.getId(), i.getInvoiceNo(), i.getOrder().getId(), i.getOrder().getOrderCode(),
                i.getOrder().getStatus(), i.getInvoiceType(), i.getParentInvoice() == null ? null : i.getParentInvoice().getId(),
                i.getStatus(), i.getBuyerName(), i.getBuyerEmail(), i.getBuyerAddress(), i.getBuyerTaxCode(),
                i.getSubtotal(), i.getDiscountAmount(), i.getShippingFee(), i.getTotalAmount(), i.getReason(),
                i.getPdfUrl(), i.getIssuedBy(), i.getIssuedAt(),
                invoiceItems.stream().sorted(Comparator.comparing(InvoiceItem::getId)).map(item -> new InvoiceItemResponse(
                        item.getId(), i.getId(), item.getDescription(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal())).toList(),
                invoiceDeliveries.stream().sorted(Comparator.comparing(InvoiceDelivery::getId).reversed()).map(this::deliveryResponse).toList());
    }

    private InvoiceDeliveryResponse deliveryResponse(InvoiceDelivery d) {
        return new InvoiceDeliveryResponse(d.getId(), d.getInvoice().getId(), d.getChannel(), d.getRecipient(), d.getStatus(), d.getSentAt());
    }
}
