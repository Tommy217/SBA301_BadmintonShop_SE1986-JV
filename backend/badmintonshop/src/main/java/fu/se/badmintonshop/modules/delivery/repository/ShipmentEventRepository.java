package fu.se.badmintonshop.modules.delivery.repository;

import fu.se.badmintonshop.modules.delivery.entity.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    List<ShipmentEvent> findByShipmentIdOrderByOccurredAtDesc(Long shipmentId);
}