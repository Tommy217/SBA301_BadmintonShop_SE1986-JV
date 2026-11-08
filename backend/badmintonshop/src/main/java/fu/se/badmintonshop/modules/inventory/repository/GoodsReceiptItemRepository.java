package fu.se.badmintonshop.modules.inventory.repository;

import java.util.List;

import fu.se.badmintonshop.modules.inventory.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Long> {
    List<GoodsReceiptItem> findByReceiptId(Long receiptId);
}
