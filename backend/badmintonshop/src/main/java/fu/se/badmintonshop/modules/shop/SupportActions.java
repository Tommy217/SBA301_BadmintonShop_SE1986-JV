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

    void execute(String type, Map<String, Object> action, AuthService.User user) {
        if ("create_support_ticket".equals(type)) {
            var request = request(action, CreateTicketRequest.class);
            request.setUserId(user.id());
            require(jdbc.update("UPDATE orders SET updated_at=updated_at WHERE id=? AND user_id=? AND status='completed' AND payment_status='paid'", request.getOrderId(), user.id()) == 1,
                    HttpStatus.CONFLICT, "Chỉ tạo yêu cầu đổi/trả cho đơn đã giao và đã thanh toán của bạn.");
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
            case "ship_exchange_support_ticket" -> {
                var request = request(action, ShipExchangeRequest.class);
                request.setStaffId(user.id());
                require(!text(action, "tracking_code").isBlank(), HttpStatus.BAD_REQUEST, "Vui lòng nhập mã vận đơn hàng thay thế.");
                require(jdbc.queryForObject("SELECT COUNT(*) FROM support_tickets t JOIN ticket_inspection i ON i.ticket_id=t.id WHERE t.id=? AND t.ticket_type='EXCHANGE' AND t.status='AWAITING_RETURN' AND i.result='PASSED'", Long.class, ticketId) == 1,
                        HttpStatus.CONFLICT, "Hàng đổi phải được thẩm định đạt trước khi xuất kho.");
                for (var item : jdbc.queryForList("SELECT replacement_variant_id,quantity FROM ticket_items WHERE ticket_id=? ORDER BY replacement_variant_id", ticketId)) {
                    long variantId = ((Number)item.get("replacement_variant_id")).longValue();
                    int quantity = ((Number)item.get("quantity")).intValue();
                    require(jdbc.update("UPDATE product_variants SET stock_quantity=stock_quantity-? WHERE id=? AND is_active=1 AND stock_quantity>=?", quantity, variantId, quantity) == 1,
                            HttpStatus.CONFLICT, "Biến thể thay thế không đủ tồn kho.");
                    jdbc.update("INSERT INTO inventory_movements(variant_id,movement_type,quantity,balance_after,ref_type,ref_id,note,created_by,created_at) VALUES(?,'stock_out',?,(SELECT stock_quantity FROM product_variants WHERE id=?),'ticket',?,'Xuất hàng đổi',?,?)",
                            variantId, -quantity, variantId, ticketId, user.id(), java.time.LocalDateTime.now());
                }
                tickets.shipExchange(ticketId, request);
            }
            case "replacement_unavailable_support_ticket" -> tickets.replacementUnavailable(ticketId, user.id(), text(action, "note"));
            case "refund_transfer_support_ticket" -> {
                var request = request(action, RefundTransferRequest.class);
                request.setStaffId(user.id());
                tickets.recordRefundTransfer(ticketId, request);
            }
            case "manager_decision_support_ticket" -> {
                require(user.roles().stream().anyMatch(Set.of("MANAGER", "ADMIN")::contains), HttpStatus.FORBIDDEN, "Chỉ quản lý được phê duyệt khoản hoàn này.");
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
