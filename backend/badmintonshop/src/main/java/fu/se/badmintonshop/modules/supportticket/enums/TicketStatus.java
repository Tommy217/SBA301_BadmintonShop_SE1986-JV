package fu.se.badmintonshop.modules.supportticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketStatus {
    PENDING("PENDING", "Chá» tiáº¿p nháº­n sÆ¡ duyá»‡t"),
    REJECTED("REJECTED", "Bá»‹ tá»« chá»‘i"),
    AWAITING_RETURN("AWAITING_RETURN", "Chá» khÃ¡ch gá»­i láº¡i hÃ ng"),
    INSPECTION_FAILED("INSPECTION_FAILED", "Kiá»ƒm tra hÃ ng khÃ´ng Ä‘áº¡t"),
    PENDING_APPROVAL("PENDING_APPROVAL", "Chá» Quáº£n lÃ½ phÃª duyá»‡t hoÃ n tiá»n"),
    PENDING_TRANSFER("PENDING_TRANSFER", "Chá» nhÃ¢n viÃªn chuyá»ƒn khoáº£n hoÃ n tiá»n"),
    REFUNDED("REFUNDED", "ÄÃ£ hoÃ n tiá»n thÃ nh cÃ´ng"),
    COMPLETED("COMPLETED", "ÄÃ£ hoÃ n táº¥t Ä‘á»•i hÃ ng"),
    CANCELLED("CANCELLED", "KhÃ¡ch Ä‘Ã£ há»§y yÃªu cáº§u");

    private final String value;
    private final String description;
}

