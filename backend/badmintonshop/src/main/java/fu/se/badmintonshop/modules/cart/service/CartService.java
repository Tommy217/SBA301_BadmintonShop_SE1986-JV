package fu.se.badmintonshop.modules.cart.service;

import java.util.Map;

import fu.se.badmintonshop.modules.cart.dto.request.*;
import fu.se.badmintonshop.modules.auth.service.AuthService;

public interface CartService {
    Map<String, Object> snapshot(Long userId);

    void execute(Map<String, Object> action, long userId);

    void addItem(CartItemRequest request, long userId);

    void updateItem(long id, CartItemRequest request, long userId);

    void removeItem(long id, long userId);

    void selectAll(CartSelectionRequest request, long userId);
}
