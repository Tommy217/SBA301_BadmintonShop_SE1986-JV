package fu.se.badmintonshop.modules.catalog.repository;

import fu.se.badmintonshop.modules.catalog.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    List<VoucherUsage> findByVoucherId(Integer voucherId);
    List<VoucherUsage> findByUserId(Long userId);
    List<VoucherUsage> findByOrderId(Long orderId);
}
