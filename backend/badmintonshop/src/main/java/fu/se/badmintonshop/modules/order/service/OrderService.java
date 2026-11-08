package fu.se.badmintonshop.modules.order.service;

import java.util.Map;
import fu.se.badmintonshop.modules.order.dto.request.*;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;

public interface OrderService {
    Map<String, Object> snapshot(CustomUserDetails user);

    Map<String, Object> customerSnapshot(CustomUserDetails user);

    Map<String, Object> voucherWallet(CustomUserDetails user);

    void execute(Map<String, Object> action, CustomUserDetails user);

    void checkout(CheckoutRequest request, CustomUserDetails user);

    void cancelOrder(long id, CancelOrderRequest request, CustomUserDetails user);

    void updateStatus(long id, UpdateOrderStatusRequest request, CustomUserDetails user);

    void startPayment(long id, StartOrderPaymentRequest request, CustomUserDetails user);

    void cancelPayment(long id, CancelOrderRequest request, CustomUserDetails user);
}
