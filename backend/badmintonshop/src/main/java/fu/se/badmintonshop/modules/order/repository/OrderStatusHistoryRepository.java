package fu.se.badmintonshop.modules.order.repository;

import java.util.List;
import java.util.Collection;
import fu.se.badmintonshop.modules.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdInOrderById(Collection<Long> orderIds);
}
