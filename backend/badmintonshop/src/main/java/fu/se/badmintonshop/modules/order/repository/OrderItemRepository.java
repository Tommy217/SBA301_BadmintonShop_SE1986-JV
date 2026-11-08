package fu.se.badmintonshop.modules.order.repository;

import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.Query;

import fu.se.badmintonshop.modules.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    boolean existsByProductId(Long productId);

    boolean existsByVariantId(Long variantId);

    @Query("SELECT i FROM OrderItem i WHERE i.orderId=?1 ORDER BY i.variantId")
    List<OrderItem> findRestockItems(Long orderId);

    List<OrderItem> findByOrderIdInOrderById(Collection<Long> orderIds);
}
