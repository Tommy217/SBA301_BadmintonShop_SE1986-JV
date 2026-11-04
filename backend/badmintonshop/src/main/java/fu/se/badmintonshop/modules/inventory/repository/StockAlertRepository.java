package fu.se.badmintonshop.modules.inventory.repository;

import fu.se.badmintonshop.modules.inventory.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    java.util.List<StockAlert> findByVariantIdAndStatus(Long variantId, String status);
}
