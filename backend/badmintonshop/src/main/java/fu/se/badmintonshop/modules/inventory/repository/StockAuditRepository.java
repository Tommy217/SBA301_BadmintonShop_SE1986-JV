package fu.se.badmintonshop.modules.inventory.repository;

import fu.se.badmintonshop.modules.inventory.entity.StockAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAuditRepository extends JpaRepository<StockAudit, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from StockAudit a where a.id = :id")
    java.util.Optional<StockAudit> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
}
