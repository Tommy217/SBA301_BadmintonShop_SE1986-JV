package fu.se.badmintonshop.modules.ticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketType {
    REFUND("REFUND", "Trả hàng & Hoàn tiền"),
    EXCHANGE("EXCHANGE", "Đổi sản phẩm");

    private final String value;
    private final String description;
}

