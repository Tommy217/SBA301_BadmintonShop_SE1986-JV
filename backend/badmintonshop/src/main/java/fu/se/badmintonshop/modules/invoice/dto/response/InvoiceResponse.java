package fu.se.badmintonshop.modules.invoice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(Long id, String invoiceNo, Long orderId, String orderCode,
        String orderStatus, String invoiceType, Long parentInvoiceId, String status,
        String buyerName, String buyerEmail, String buyerAddress, String buyerTaxCode,
        BigDecimal subtotal, BigDecimal discountAmount, BigDecimal shippingFee,
        BigDecimal totalAmount, String reason, String pdfUrl, Long issuedBy,
        LocalDateTime issuedAt, List<InvoiceItemResponse> items,
        List<InvoiceDeliveryResponse> deliveries) {}
