package fu.se.badmintonshop.modules.supportticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketCause {
    STORE_FAULT("STORE_FAULT", "Lá»—i do cá»­a hÃ ng hoáº·c nhÃ  sáº£n xuáº¥t (HoÃ n phÃ­ váº­n chuyá»ƒn)"),
    CUSTOMER_PREFERENCE("CUSTOMER_PREFERENCE", "Nhu cáº§u cÃ¡ nhÃ¢n cá»§a khÃ¡ch hÃ ng (KhÃ¡ch chá»‹u phÃ­ váº­n chuyá»ƒn)");

    private final String value;
    private final String description;
}

