package fu.se.badmintonshop.modules.payment.controller;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PaymentAccess {
    private final OrderRepository orders;
    private final PaymentRepository payments;

    public void order(Long orderId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        Long userId = principal instanceof CustomUserDetails user ? user.getId()
                : principal instanceof AuthService.User user ? user.id() : null;
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        var order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
        if (!userId.equals(order.getUserId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập đơn hàng này.");
    }

    public void payment(Long paymentId) {
        var payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thanh toán."));
        order(payment.getOrderId());
    }
}
