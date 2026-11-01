package fu.se.badmintonshop.modules.supportticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InspectionResult {
    PASSED("PASSED", "Äáº¡t yÃªu cáº§u tháº©m Ä‘á»‹nh"),
    FAILED("FAILED", "KhÃ´ng Ä‘áº¡t yÃªu cáº§u tháº©m Ä‘á»‹nh");

    private final String value;
    private final String description;
}

