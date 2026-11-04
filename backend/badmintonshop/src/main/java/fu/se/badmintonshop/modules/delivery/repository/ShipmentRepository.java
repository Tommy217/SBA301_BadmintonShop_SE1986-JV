package fu.se.badmintonshop.modules.delivery.repository;

import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByTrackingCode(String trackingCode);

    List<Shipment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByOrderIdAndStatusNot(Long orderId, String status);

    // con van don hieu luc (khong nam trong cac trang thai da ket thuc vo hieu)
    boolean existsByOrderIdAndStatusNotIn(Long orderId, Collection<String> statuses);

    // nap san order + carrier cho trang danh sach, tranh N+1
    @Override
    @EntityGraph(attributePaths = {"order", "carrier"})
    Page<Shipment> findAll(Specification<Shipment> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "carrier", "events"})
    Optional<Shipment> findWithDetailsById(Long id);
}