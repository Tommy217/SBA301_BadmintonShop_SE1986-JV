package fu.se.badmintonshop.modules.inventory.repository;

import fu.se.badmintonshop.modules.inventory.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Long> {
    java.util.List<GoodsReceiptItem> findByReceiptId(Long receiptId);
}
