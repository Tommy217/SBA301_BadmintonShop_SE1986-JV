package fu.se.badmintonshop.modules.inventory.repository;

import fu.se.badmintonshop.modules.inventory.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    boolean existsBySupplierId(Integer supplierId);
}
