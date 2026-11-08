package fu.se.badmintonshop.modules.cart.repository;

import java.util.Optional;
import fu.se.badmintonshop.modules.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}
