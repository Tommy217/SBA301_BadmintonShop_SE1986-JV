package fu.se.badmintonshop.modules.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import fu.se.badmintonshop.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartIdOrderById(Long cartId);

    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    @Query("SELECT i FROM CartItem i, Cart c WHERE c.id=i.cartId AND c.userId=?1 ORDER BY i.variantId")
    List<CartItem> findOwnedItems(Long userId);

}
