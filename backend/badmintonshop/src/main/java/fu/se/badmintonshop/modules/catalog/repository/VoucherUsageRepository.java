package fu.se.badmintonshop.modules.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import fu.se.badmintonshop.modules.catalog.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    List<VoucherUsage> findByVoucherId(Integer voucherId);

    List<VoucherUsage> findByUserId(Long userId);

    List<VoucherUsage> findByOrderId(Long orderId);

    @Query("SELECT COUNT(v) FROM VoucherUsage v WHERE v.voucherId=?1 AND v.userId=?2")
    long countUserUsage(Integer voucherId, Long userId);

    @Query("SELECT v FROM VoucherUsage v WHERE v.userId=?1")
    List<VoucherUsage> findUserUsage(Long userId);
}
