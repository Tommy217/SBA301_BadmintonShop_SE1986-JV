package fu.se.badmintonshop.modules.shop;

import com.fasterxml.jackson.databind.*;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import fu.se.badmintonshop.modules.supportticket.dto.*;
import fu.se.badmintonshop.modules.supportticket.service.SupportTicketService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static fu.se.badmintonshop.modules.shop.ShopService.*;

@Service
@RequiredArgsConstructor
class SupportActions {
    private final SupportTicketService tickets;
    private final Validator validator;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    SupportTicketService getTickets() {
        return tickets;
    }

    void execute(String type, Map<String, Object> action, AuthService.User user) {
        if ("update_refund_threshold".equals(type)) {
            require(user != null && Set.of("MANAGER", "ADMIN").contains(user.role()), HttpStatus.FORBIDDEN, "Chỉ quản lý được thiết lập ngưỡng phê duyệt hoàn tiền.");
            java.math.BigDecimal newThreshold = new java.math.BigDecimal(String.valueOf(action.get("threshold")));
            tickets.updateApprovalThreshold(newThreshold, user.id());
            return;
        }
        if ("create_support_ticket".equals(type)) {
            var request = request(action, CreateTicketRequest.class);
            request.setUserId(user.id());
            require(jdbc.update("UPDATE orders SET updated_at=updated_at WHERE id=? AND user_id=? AND payment_status='paid' AND status NOT IN ('cancelled', 'returned')", request.getOrderId(), user.id()) == 1,
                    HttpStatus.CONFLICT, "Chỉ tạo yêu cầu hỗ trợ cho đơn đã thanh toán hợp lệ của bạn.");
            var seen = new HashSet<Long>();
            require(request.getItems().stream().allMatch(i -> seen.add(i.getOrderItemId())), HttpStatus.BAD_REQUEST, "Không chọn trùng sản phẩm trong phiếu.");
            tickets.createTicket(request, user.id());
            return;
        }
        if ("cancel_support_ticket".equals(type)) {
            tickets.cancelTicket(id(action.get("ticket_id")), user.id());
            return;
        }
        require(management(user), HttpStatus.FORBIDDEN, "Bạn không có quyền xử lý phiếu hỗ trợ.");
        long ticketId = id(action.get("ticket_id"));
        require(jdbc.update("UPDATE support_tickets SET updated_at=updated_at WHERE id=?", ticketId) == 1, HttpStatus.NOT_FOUND, "Không tìm thấy phiếu hỗ trợ.");
        switch (type) {
            case "review_support_ticket" -> {
                var request = request(action, ReviewTicketRequest.class);
                request.setStaffId(user.id());
                tickets.reviewTicket(ticketId, request);
            }
            case "inspect_support_ticket" -> {
                var request = request(action, InspectionRequest.class);
                request.setStaffId(user.id());
                tickets.inspectTicket(ticketId, request);
            }
            case "approve_refund_support_ticket" -> tickets.approveRefund(ticketId, user.id());
            case "refund_transfer_support_ticket" -> {
                var request = request(action, RefundTransferRequest.class);
                request.setStaffId(user.id());
                tickets.recordRefundTransfer(ticketId, request);
            }
            case "manager_decision_support_ticket" -> {
                require(user != null && Set.of("MANAGER", "ADMIN").contains(user.role()), HttpStatus.FORBIDDEN, "Chỉ quản lý được phê duyệt khoản hoàn này.");
                var request = request(action, ManagerDecisionRequest.class);
                request.setManagerId(user.id());
                tickets.managerDecision(ticketId, request);
            }
            default -> require(false, HttpStatus.BAD_REQUEST, "Thao tác không được hỗ trợ.");
        }
    }

    private <T> T request(Map<String, Object> action, Class<T> type) {
        T request = mapper.convertValue(action, type);
        var violations = validator.validate(request);
        require(violations.isEmpty(), HttpStatus.BAD_REQUEST, violations.stream().map(v -> v.getMessage()).findFirst().orElse("Dữ liệu không hợp lệ."));
        return request;
    }
}
