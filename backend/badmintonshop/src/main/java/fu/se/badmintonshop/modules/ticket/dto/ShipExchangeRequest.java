package fu.se.badmintonshop.modules.ticket.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipExchangeRequest {

    private String trackingCode;
    private String shippingNote;
    private Long staffId;
}

