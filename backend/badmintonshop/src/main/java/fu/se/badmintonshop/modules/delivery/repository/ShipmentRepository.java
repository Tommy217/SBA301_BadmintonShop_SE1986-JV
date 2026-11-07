package fu.se.badmintonshop.modules.delivery.repository;

import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // van don cua cac don hang thuoc 1 khach
    @EntityGraph(attributePaths = {"order", "carrier"})
    List<Shipment> findByOrderUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"order", "carrier", "events"})
    Optional<Shipment> findWithDetailsById(Long id);

    // job dong bo dinh ky: van don da gui doi tac, chua ket thuc; lau chua dong bo nhat truoc
    @Query("select s.id from Shipment s where s.partnerRef is not null and s.status not in :finalStatuses order by s.lastSyncedAt asc")
    List<Long> findIdsForCarrierSync(@Param("finalStatuses") Collection<String> finalStatuses, Pageable pageable);
}
