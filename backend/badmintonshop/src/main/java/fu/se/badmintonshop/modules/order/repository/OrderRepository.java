package fu.se.badmintonshop.modules.order.repository;

import fu.se.badmintonshop.modules.order.entity.Order;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT e FROM Order e WHERE e.id=?1")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findLockedById(Long id);

    @Query("SELECT o FROM Order o WHERE o.id=?1 AND o.userId=?2")
    Optional<Order> findOwnedOrder(Long id, Long userId);

    @Query(
            "UPDATE Order SET status=?1,paymentStatus=?2,updatedAt=?3 WHERE id=?4 AND status=?5 AND paymentStatus=?6")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int updateExpectedStatus(
            String status,
            String paymentStatus,
            LocalDateTime updatedAt,
            Long id,
            String expectedStatus,
            String expectedPayment);

    @Query("UPDATE Order SET confirmedBy=?1,confirmedAt=?2 WHERE id=?3")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int recordConfirmation(Long actor, LocalDateTime time, Long id);

    @Query("UPDATE Order SET cancelledBy=?1,cancelledAt=?2,cancelReason=?3 WHERE id=?4")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    int recordCancellation(Long actor, LocalDateTime time, String reason, Long id);

    @Query(
            "SELECT COUNT(o) FROM Order o WHERE o.id=?1 AND o.userId=?2 AND o.paymentStatus='paid' AND o.status NOT IN ('cancelled','returned')")
    long countSupportEligible(Long id, Long userId);

    List<Order> findAllByOrderById();

    List<Order> findByUserIdOrderById(Long userId);
}
