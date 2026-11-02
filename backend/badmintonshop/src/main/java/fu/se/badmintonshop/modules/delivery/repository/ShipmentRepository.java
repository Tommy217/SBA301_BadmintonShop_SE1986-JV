package fu.se.badmintonshop.modules.delivery.repository;

import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByTrackingCode(String trackingCode);

    List<Shipment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByOrderIdAndStatusNot(Long orderId, String status);

    @EntityGraph(attributePaths = {"order", "carrier", "events"})
    Optional<Shipment> findWithDetailsById(Long id);
}