package fu.se.badmintonshop.modules.supportticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InspectionResult {
    PASSED("PASSED", "Đạt yêu cầu thẩm định"),
    FAILED("FAILED", "Không đạt yêu cầu thẩm định");

    private final String value;
    private final String description;
}

