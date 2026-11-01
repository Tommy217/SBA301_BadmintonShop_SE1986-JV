package fu.se.badmintonshop.modules.support_ticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketStatus {
    PENDING("PENDING", "Chờ tiếp nhận sơ duyệt"),
    REJECTED("REJECTED", "Bị từ chối"),
    AWAITING_RETURN("AWAITING_RETURN", "Chờ khách gửi lại hàng"),
    INSPECTION_FAILED("INSPECTION_FAILED", "Kiểm tra hàng không đạt"),
    PENDING_APPROVAL("PENDING_APPROVAL", "Chờ Quản lý phê duyệt hoàn tiền"),
    PENDING_TRANSFER("PENDING_TRANSFER", "Chờ nhân viên chuyển khoản hoàn tiền"),
    REFUNDED("REFUNDED", "Đã hoàn tiền thành công"),
    COMPLETED("COMPLETED", "Đã hoàn tất đổi hàng"),
    CANCELLED("CANCELLED", "Khách đã hủy yêu cầu");

    private final String value;
    private final String description;
}

