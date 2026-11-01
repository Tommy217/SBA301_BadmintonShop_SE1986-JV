package fu.se.badmintonshop.modules.support_ticket.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketItemResponse {
    private Long id;
    private Long orderItemId;
    private String productName;
    private String variantInfo;
    private BigDecimal unitPrice;
    private Integer quantity;
    private Long replacementVariantId;
    private String replacementVariantSku;
    private String replacementVariantInfo;
}

