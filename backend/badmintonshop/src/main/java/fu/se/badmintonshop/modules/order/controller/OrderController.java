package fu.se.badmintonshop.modules.order.controller;

import fu.se.badmintonshop.modules.cart.service.CartService;
import fu.se.badmintonshop.modules.common.mapper.SnapshotResponseMapper;
import fu.se.badmintonshop.modules.common.service.CurrentUser;
import fu.se.badmintonshop.modules.order.dto.request.*;
import fu.se.badmintonshop.modules.order.dto.response.*;
import fu.se.badmintonshop.modules.order.service.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orders;
    private final CartService carts;

    @GetMapping
    public OrderSnapshotResponse snapshot(Authentication auth) {
        return SnapshotResponseMapper.order(orders.snapshot(CurrentUser.from(auth)));
    }

    @GetMapping("/mine")
    public OrderSnapshotResponse mine(Authentication auth) {
        return SnapshotResponseMapper.order(orders.customerSnapshot(CurrentUser.from(auth)));
    }

    @GetMapping("/vouchers")
    public VoucherWalletResponse vouchers(Authentication auth) {
        return SnapshotResponseMapper.wallet(orders.voucherWallet(CurrentUser.from(auth)));
    }

    @PostMapping
    public OrderSnapshotResponse checkout(
            @RequestBody CheckoutRequest request, Authentication auth) {
        var user = CurrentUser.from(auth);
        orders.checkout(request, user);
        var result = new LinkedHashMap<>(orders.snapshot(user));
        result.putAll(carts.snapshot(user.id()));
        return SnapshotResponseMapper.order(result);
    }

    @PostMapping("/{id}/cancel")
    public OrderSnapshotResponse cancel(
            @PathVariable long id, @RequestBody CancelOrderRequest request, Authentication auth) {
        var user = CurrentUser.from(auth);
        orders.cancelOrder(id, request, user);
        return SnapshotResponseMapper.order(orders.snapshot(user));
    }

    @PatchMapping("/{id}/status")
    public OrderSnapshotResponse status(
            @PathVariable long id,
            @RequestBody UpdateOrderStatusRequest request,
            Authentication auth) {
        var user = CurrentUser.from(auth);
        orders.updateStatus(id, request, user);
        return SnapshotResponseMapper.order(orders.snapshot(user));
    }

    @PostMapping("/{id}/payment")
    public OrderSnapshotResponse payment(
            @PathVariable long id,
            @RequestBody StartOrderPaymentRequest request,
            Authentication auth) {
        var user = CurrentUser.from(auth);
        orders.startPayment(id, request, user);
        return SnapshotResponseMapper.order(orders.snapshot(user));
    }

    @PostMapping("/{id}/payment/cancel")
    public OrderSnapshotResponse cancelPayment(
            @PathVariable long id, @RequestBody CancelOrderRequest request, Authentication auth) {
        var user = CurrentUser.from(auth);
        orders.cancelPayment(id, request, user);
        return SnapshotResponseMapper.order(orders.snapshot(user));
    }
}
