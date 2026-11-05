package fu.se.badmintonshop.modules.invoice.dto.response;

import java.time.LocalDateTime;

public record InvoiceDeliveryResponse(Long id, Long invoiceId, String channel,
                                      String recipient, String status, LocalDateTime sentAt) {}
