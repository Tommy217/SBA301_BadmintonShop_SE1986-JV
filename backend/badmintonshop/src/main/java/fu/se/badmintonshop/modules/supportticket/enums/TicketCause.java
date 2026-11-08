package fu.se.badmintonshop.modules.supportticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketCause {
    STORE_FAULT("STORE_FAULT", "Lỗi do cửa hàng hoặc nhà sản xuất (Hoàn phí vận chuyển)"),
    CUSTOMER_PREFERENCE("CUSTOMER_PREFERENCE", "Nhu cầu cá nhân của khách hàng (Khách chịu phí vận chuyển)");

    private final String value;
    private final String description;
}

