package fu.se.badmintonshop.modules.supportticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketType {
    REFUND("REFUND", "Tráº£ hÃ ng & HoÃ n tiá»n"),
    EXCHANGE("EXCHANGE", "Äá»•i sáº£n pháº©m");

    private final String value;
    private final String description;
}

