package fu.se.badmintonshop.modules.cart.controller;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.cart.dto.request.*;
import fu.se.badmintonshop.modules.cart.dto.response.CartSnapshotResponse;
import fu.se.badmintonshop.modules.cart.exception.CartException;
import fu.se.badmintonshop.modules.cart.service.CartService;
import fu.se.badmintonshop.modules.common.mapper.SnapshotResponseMapper;
import fu.se.badmintonshop.modules.common.service.CurrentUser;
import fu.se.badmintonshop.modules.order.dto.response.OrderSnapshotResponse;
import fu.se.badmintonshop.modules.order.service.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService carts;
    private final OrderService orders;

    @GetMapping
    public CartSnapshotResponse snapshot(Authentication authentication) {
        var user = CurrentUser.from(authentication);
        return SnapshotResponseMapper.cart(carts.snapshot(user == null ? null : user.id()));
    }

    @PostMapping("/items")
    public OrderSnapshotResponse add(
            @RequestBody CartItemRequest request, Authentication authentication) {
        var user = user(authentication);
        carts.addItem(request, user.id());
        return updatedSnapshot(user);
    }

    @PatchMapping("/items/{id}")
    public OrderSnapshotResponse update(
            @PathVariable long id,
            @RequestBody CartItemRequest request,
            Authentication authentication) {
        var user = user(authentication);
        carts.updateItem(id, request, user.id());
        return updatedSnapshot(user);
    }

    @DeleteMapping("/items/{id}")
    public OrderSnapshotResponse remove(@PathVariable long id, Authentication authentication) {
        var user = user(authentication);
        carts.removeItem(id, user.id());
        return updatedSnapshot(user);
    }

    @PatchMapping("/selection")
    public OrderSnapshotResponse selection(
            @RequestBody CartSelectionRequest request, Authentication authentication) {
        var user = user(authentication);
        carts.selectAll(request, user.id());
        return updatedSnapshot(user);
    }

    private CustomUserDetails user(Authentication authentication) {
        var user = CurrentUser.from(authentication);
        if (user == null) throw new CartException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        return user;
    }

    private OrderSnapshotResponse updatedSnapshot(CustomUserDetails user) {
        var result = new LinkedHashMap<>(orders.snapshot(user));
        result.putAll(carts.snapshot(user.id()));
        return SnapshotResponseMapper.order(result);
    }
}
