package fu.se.badmintonshop.modules.inventory.repository;

import fu.se.badmintonshop.modules.inventory.entity.StockAuditItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAuditItemRepository extends JpaRepository<StockAuditItem, Long> {
    java.util.List<StockAuditItem> findByAuditIdOrderByVariantId(Long auditId);
}
