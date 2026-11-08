package fu.se.badmintonshop.modules.inventory.repository;

import java.util.List;

import fu.se.badmintonshop.modules.inventory.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findByVariantIdAndStatus(Long variantId, String status);
}
