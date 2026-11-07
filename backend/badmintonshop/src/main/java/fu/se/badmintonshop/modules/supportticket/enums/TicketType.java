package fu.se.badmintonshop.modules.supportticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketType {
    REFUND("REFUND", "Trả hàng & Hoàn tiền");

    private final String value;
    private final String description;
}
