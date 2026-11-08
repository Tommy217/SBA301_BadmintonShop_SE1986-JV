package fu.se.badmintonshop.modules.inventory.repository;

import java.util.List;

import fu.se.badmintonshop.modules.inventory.entity.StockAuditItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAuditItemRepository extends JpaRepository<StockAuditItem, Long> {
    List<StockAuditItem> findByAuditIdOrderByVariantId(Long auditId);
}
