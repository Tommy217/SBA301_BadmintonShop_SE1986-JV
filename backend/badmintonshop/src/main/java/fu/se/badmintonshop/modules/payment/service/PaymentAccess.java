package fu.se.badmintonshop.modules.payment.service;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.common.service.CommerceValidation;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentAccess {
    private final OrderRepository orders;
    private final PaymentRepository payments;

    @Transactional(readOnly = true)
    public void order(Long orderId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        Long userId = principal instanceof CustomUserDetails user ? user.getId() : null;
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }

        var order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));

        if (principal instanceof CustomUserDetails user && CommerceValidation.management(user)) {
            return;
        }

        if (!userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập đơn hàng này.");
        }
    }

    @Transactional(readOnly = true)
    public void payment(Long paymentId) {
        var payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thanh toán."));
        order(payment.getOrderId());
    }
}

