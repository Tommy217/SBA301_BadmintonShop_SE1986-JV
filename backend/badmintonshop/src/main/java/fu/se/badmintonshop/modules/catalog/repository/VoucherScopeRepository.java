package fu.se.badmintonshop.modules.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import fu.se.badmintonshop.modules.catalog.entity.VoucherScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherScopeRepository extends JpaRepository<VoucherScope, Integer> {
    List<VoucherScope> findByVoucherId(Integer voucherId);

    boolean existsByVoucherId(Integer voucherId);

    boolean existsByScopeTypeAndScopeId(String scopeType, Long scopeId);

    boolean existsByVoucherIdAndScopeTypeAndScopeId(
            Integer voucherId, String scopeType, Long scopeId);

    boolean existsByVoucherIdAndScopeTypeAndScopeIdAndIdNot(
            Integer voucherId, String scopeType, Long scopeId, Integer id);

    @Query("SELECT s FROM VoucherScope s WHERE s.voucher.id=?1")
    List<VoucherScope> findVoucherScopes(Integer voucherId);
}
