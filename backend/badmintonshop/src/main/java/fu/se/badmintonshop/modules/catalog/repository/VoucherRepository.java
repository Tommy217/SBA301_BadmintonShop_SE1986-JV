package fu.se.badmintonshop.modules.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import fu.se.badmintonshop.modules.catalog.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);

    Optional<Voucher> findByCode(String code);

    @Query(
            "UPDATE Voucher SET usedCount=usedCount+1 WHERE UPPER(code)=?1 AND isActive=true AND startsAt<=?2 AND endsAt>=?3 AND (usageLimit IS NULL OR usedCount<usageLimit)")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int consumeAvailableCode(String code, LocalDateTime start, LocalDateTime end);

    @Query(
            "SELECT COUNT(v) FROM Voucher v WHERE (v.audience='public' OR EXISTS (SELECT uv.userId FROM UserVoucher uv WHERE uv.voucherId=v.id AND uv.userId=?1)) AND UPPER(v.code)=?2")
    long countAccessibleCode(Long userId, String code);

    @Query("SELECT v FROM Voucher v WHERE UPPER(v.code)=?1")
    Optional<Voucher> findByUpperCode(String code);

    @Query(
            "SELECT v FROM Voucher v WHERE v.audience='public' OR EXISTS (SELECT uv.userId FROM UserVoucher uv WHERE uv.voucherId=v.id AND uv.userId=?1) ORDER BY v.endsAt,v.id")
    List<Voucher> findWallet(Long userId);
}
