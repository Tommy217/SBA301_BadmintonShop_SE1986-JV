package fu.se.badmintonshop.modules.supportticket.service;

import com.fasterxml.jackson.databind.*;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.payment.service.PaymentService;
import fu.se.badmintonshop.modules.supportticket.dto.*;
import fu.se.badmintonshop.modules.supportticket.service.SupportTicketService;
import jakarta.validation.Validator;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import fu.se.badmintonshop.modules.common.service.EntityLockService;
import static fu.se.badmintonshop.modules.common.mapper.SnapshotMapper.*;
import java.util.*;
import static fu.se.badmintonshop.modules.common.service.CommerceValidation.*;

@Service
@RequiredArgsConstructor
public class SupportActions {
    private final SupportTicketService tickets;
    private final PaymentService payments;
    private final Validator validator;
    private final OrderRepository orderRepository;
    private final EntityLockService locks;
    private final ObjectMapper mapper =
            new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public SupportTicketService getTickets() {
        return tickets;
    }

    @Transactional
    public void execute(String type, Map<String, Object> action, CustomUserDetails user) {
        require(user != null, HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        if ("update_refund_threshold".equals(type)) {
            require(
                    user != null && Set.of("MANAGER", "ADMIN").contains(user.role()),
                    HttpStatus.FORBIDDEN,
                    "Chỉ quản lý được thiết lập ngưỡng phê duyệt hoàn tiền.");
            java.math.BigDecimal newThreshold =
                    new java.math.BigDecimal(String.valueOf(action.get("threshold")));
            tickets.updateApprovalThreshold(newThreshold, user.id());
            return;
        }
        if ("update_payment_timeout".equals(type)) {
            require(
                    user != null && Set.of("MANAGER", "ADMIN").contains(user.role()),
                    HttpStatus.FORBIDDEN,
                    "Chỉ quản lý được thiết lập thời gian hủy mã QR.");
            Object minObj = action.get("timeout_minutes");
            if (minObj == null) minObj = action.get("timeoutMinutes");
            int minutes = Integer.parseInt(String.valueOf(minObj));
            payments.updatePaymentTimeoutMinutes(minutes, user.id());
            return;
        }
        if ("create_support_ticket".equals(type)) {
            var request = request(action, CreateTicketRequest.class);
            request.setUserId(user.id());
            locks.lockOrder(request.getOrderId());
            require(
                    orderRepository.countSupportEligible(request.getOrderId(), user.id()) == 1,
                    HttpStatus.CONFLICT,
                    "Chỉ tạo yêu cầu hỗ trợ cho đơn đã thanh toán hợp lệ của bạn.");
            var seen = new HashSet<Long>();
            require(
                    request.getItems().stream().allMatch(i -> seen.add(i.getOrderItemId())),
                    HttpStatus.BAD_REQUEST,
                    "Không chọn trùng sản phẩm trong phiếu.");
            tickets.createTicket(request, user.id());
            return;
        }
        if ("cancel_support_ticket".equals(type)) {
            tickets.cancelTicket(id(action.get("ticket_id")), user.id());
            return;
        }
        require(management(user), HttpStatus.FORBIDDEN, "Bạn không có quyền xử lý phiếu hỗ trợ.");
        long ticketId = id(action.get("ticket_id"));
        require(locks.lockTicket(ticketId), HttpStatus.NOT_FOUND, "Không tìm thấy phiếu hỗ trợ.");
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
                require(
                        user != null && Set.of("MANAGER", "ADMIN").contains(user.role()),
                        HttpStatus.FORBIDDEN,
                        "Chỉ quản lý được phê duyệt khoản hoàn này.");
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
        require(
                violations.isEmpty(),
                HttpStatus.BAD_REQUEST,
                violations.stream()
                        .map(v -> v.getMessage())
                        .findFirst()
                        .orElse("Dữ liệu không hợp lệ."));
        return request;
    }
}
