package fu.se.badmintonshop.modules.order.service;

import java.util.Map;
import fu.se.badmintonshop.modules.order.dto.request.*;
import fu.se.badmintonshop.modules.auth.service.AuthService;

public interface OrderService {
    Map<String, Object> snapshot(fu.se.badmintonshop.modules.auth.service.AuthService.User user);

    Map<String, Object> customerSnapshot(
            fu.se.badmintonshop.modules.auth.service.AuthService.User user);

    Map<String, Object> voucherWallet(
            fu.se.badmintonshop.modules.auth.service.AuthService.User user);

    void execute(
            Map<String, Object> action,
            fu.se.badmintonshop.modules.auth.service.AuthService.User user);

    void checkout(CheckoutRequest request, AuthService.User user);

    void cancelOrder(long id, CancelOrderRequest request, AuthService.User user);

    void updateStatus(long id, UpdateOrderStatusRequest request, AuthService.User user);

    void startPayment(long id, StartOrderPaymentRequest request, AuthService.User user);

    void cancelPayment(long id, CancelOrderRequest request, AuthService.User user);
}
