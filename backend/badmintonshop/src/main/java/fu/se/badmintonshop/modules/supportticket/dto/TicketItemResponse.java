package fu.se.badmintonshop.modules.supportticket.dto;

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
}

