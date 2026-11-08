package fu.se.badmintonshop.modules.payment.repository;

import java.util.List;
import java.util.Collection;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import fu.se.badmintonshop.modules.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, String status);

    Optional<Payment> findByGatewayTxnRef(String gatewayTxnRef);

    List<Payment> findByStatusAndCreatedAtBefore(String status, LocalDateTime cutoff);

    List<Payment> findByStatusAndCreatedAtAfter(String status, LocalDateTime cutoff);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.orderId=?1 AND p.method='cod'")
    long countCodPayments(Long orderId);

    @Query(
            "UPDATE Payment SET status='paid',paidAt=?1,updatedAt=?2 WHERE id=(SELECT MAX(p.id) FROM Payment p WHERE p.orderId=?3 AND p.status='pending')")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int markLatestPendingPaid(LocalDateTime paidAt, LocalDateTime updatedAt, Long orderId);

    @Query("UPDATE Payment SET status='refunded',updatedAt=?1 WHERE orderId=?2 AND status='paid'")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int markPaidRefunded(LocalDateTime time, Long orderId);

    @Query(
            "UPDATE Payment SET status='cancelled',cancelledAt=?1,updatedAt=?2 WHERE orderId=?3 AND status='pending'")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int cancelPending(LocalDateTime cancelledAt, LocalDateTime updatedAt, Long orderId);

    @Query("UPDATE Payment SET checkoutUrl=?1 WHERE id=?2")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int updateCheckoutUrl(String url, Long id);

    List<Payment> findByOrderIdInOrderById(Collection<Long> orderIds);
}
