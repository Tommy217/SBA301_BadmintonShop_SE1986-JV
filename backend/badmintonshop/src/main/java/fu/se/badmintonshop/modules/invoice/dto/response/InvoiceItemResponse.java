package fu.se.badmintonshop.modules.invoice.dto.response;

import java.math.BigDecimal;

public record InvoiceItemResponse(Long id, Long invoiceId, String description,
                                  BigDecimal unitPrice, Integer quantity, BigDecimal lineTotal) {}
