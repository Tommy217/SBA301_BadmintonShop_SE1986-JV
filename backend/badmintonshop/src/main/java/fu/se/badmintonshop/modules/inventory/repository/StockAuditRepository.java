package fu.se.badmintonshop.modules.inventory.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import fu.se.badmintonshop.modules.inventory.entity.StockAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAuditRepository extends JpaRepository<StockAudit, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from StockAudit a where a.id = :id")
    Optional<StockAudit> findLockedById(
            @org.springframework.data.repository.query.Param("id") Long id);
}
