package fu.se.badmintonshop.modules.cart.service.impl;

import static fu.se.badmintonshop.modules.common.mapper.SnapshotMapper.*;
import static fu.se.badmintonshop.modules.common.service.CommerceValidation.*;

import fu.se.badmintonshop.modules.cart.dto.request.*;
import fu.se.badmintonshop.modules.cart.entity.*;
import fu.se.badmintonshop.modules.cart.exception.CartException;
import fu.se.badmintonshop.modules.cart.repository.*;
import fu.se.badmintonshop.modules.cart.service.CartService;
import fu.se.badmintonshop.modules.catalog.repository.ProductRepository;
import fu.se.badmintonshop.modules.catalog.repository.ProductVariantRepository;
import fu.se.badmintonshop.modules.common.mapper.SnapshotMapper;
import fu.se.badmintonshop.modules.common.service.CommerceValidation;
import fu.se.badmintonshop.modules.common.service.EntityLockService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final EntityLockService locks;
    private final CartRepository carts;
    private final CartItemRepository items;

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot(Long userId) {
        if (userId == null) return Map.of("carts", List.of(), "cart_items", List.of());
        var cart = carts.findByUserId(userId);
        return Map.of(
                "carts",
                cart.stream().map(SnapshotMapper::of).toList(),
                "cart_items",
                cart.map(
                                c ->
                                        items.findByCartIdOrderById(c.getId()).stream()
                                                .map(SnapshotMapper::of)
                                                .toList())
                        .orElseGet(List::of));
    }

    public void execute(Map<String, Object> action, long userId) {
        locks.lockUser(userId);
        Cart cart =
                carts.findByUserId(userId)
                        .orElseGet(
                                () ->
                                        carts.save(
                                                Cart.builder()
                                                        .userId(userId)
                                                        .updatedAt(now())
                                                        .build()));
        switch (text(action, "type")) {
            case "add" -> {
                long variantId = id(action.get("variant_id"));
                var variant = variant(variantId);
                CartItem item =
                        items.findByCartIdAndVariantId(cart.getId(), variantId)
                                .orElseGet(
                                        () ->
                                                CartItem.builder()
                                                        .cartId(cart.getId())
                                                        .variantId(variantId)
                                                        .quantity(0)
                                                        .selected(true)
                                                        .addedAt(now())
                                                        .build());
                long quantity =
                        (long) item.getQuantity() + positiveInt(action.getOrDefault("quantity", 1));
                require(
                        quantity <= number(variant.get("stock_quantity")),
                        HttpStatus.CONFLICT,
                        "Sản phẩm không đủ tồn kho.");
                item.setQuantity((int) quantity);
                items.save(item);
            }
            case "select_all" -> {
                require(
                        action.get("selected") instanceof Boolean,
                        HttpStatus.BAD_REQUEST,
                        "Lựa chọn không hợp lệ.");
                for (CartItem item : items.findByCartIdOrderById(cart.getId()))
                    item.setSelected((Boolean) action.get("selected"));
            }
            case "update", "remove" -> {
                CartItem item =
                        items.findByIdAndCartId(id(action.get("id")), cart.getId())
                                .orElseThrow(
                                        () ->
                                                new CartException(
                                                        HttpStatus.NOT_FOUND,
                                                        "Không tìm thấy dữ liệu hoặc bạn không có quyền truy cập."));
                if ("remove".equals(action.get("type"))) items.delete(item);
                else {
                    if (action.containsKey("quantity")) {
                        int quantity = positiveInt(action.get("quantity"));
                        require(
                                quantity
                                        <= number(
                                                variant(item.getVariantId()).get("stock_quantity")),
                                HttpStatus.CONFLICT,
                                "Số lượng vượt tồn kho.");
                        item.setQuantity(quantity);
                    }
                    if (action.containsKey("is_selected")) {
                        require(
                                action.get("is_selected") instanceof Boolean,
                                HttpStatus.BAD_REQUEST,
                                "Lựa chọn không hợp lệ.");
                        item.setSelected((Boolean) action.get("is_selected"));
                    }
                }
            }
            default -> require(false, HttpStatus.BAD_REQUEST, "Thao tác không được hỗ trợ.");
        }
        cart.setUpdatedAt(now());
    }

    private Map<String, Object> variant(long variantId) {
        var result =
                toRow(
                        productVariantRepository
                                .findAvailableById(variantId)
                                .orElseThrow(() -> notFound()));
        var product =
                toRow(
                        productRepository
                                .findById(id(result.get("product_id")))
                                .orElseThrow(() -> notFound()));
        result.put("name", product.get("name"));
        result.put("brand_id", product.get("brand_id"));
        result.put("category_id", product.get("category_id"));
        return result;
    }

    @Override
    @Transactional
    public void addItem(CartItemRequest request, long userId) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (request.getVariantId() != null) action.put("variant_id", request.getVariantId());
        if (request.getQuantity() != null) action.put("quantity", request.getQuantity());
        if (request.getIsSelected() != null) action.put("is_selected", request.getIsSelected());
        action.put("type", "add");
        execute(action, userId);
    }

    @Override
    @Transactional
    public void updateItem(long id, CartItemRequest request, long userId) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (request.getVariantId() != null) action.put("variant_id", request.getVariantId());
        if (request.getQuantity() != null) action.put("quantity", request.getQuantity());
        if (request.getIsSelected() != null) action.put("is_selected", request.getIsSelected());
        action.put("type", "update");
        action.put("id", id);
        execute(action, userId);
    }

    @Override
    @Transactional
    public void removeItem(long id, long userId) {
        var action = new LinkedHashMap<String, Object>();
        action.put("type", "remove");
        action.put("id", id);
        execute(action, userId);
    }

    @Override
    @Transactional
    public void selectAll(CartSelectionRequest request, long userId) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (request.getSelected() != null) action.put("selected", request.getSelected());
        action.put("type", "select_all");
        execute(action, userId);
    }

    private static void require(boolean condition, HttpStatus status, String message) {
        if (!condition) throw new CartException(status, message);
    }

    private static CartException notFound() {
        return new CartException(
                HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu hoặc bạn không có quyền truy cập.");
    }

    private static long id(Object value) {
        try {
            return CommerceValidation.id(value);
        } catch (fu.se.badmintonshop.modules.common.exception.BusinessException e) {
            throw new CartException(e.getStatusCode().value(), e.getReason());
        }
    }

    private static int positiveInt(Object value) {
        try {
            return CommerceValidation.positiveInt(value);
        } catch (fu.se.badmintonshop.modules.common.exception.BusinessException e) {
            throw new CartException(e.getStatusCode().value(), e.getReason());
        }
    }
}
