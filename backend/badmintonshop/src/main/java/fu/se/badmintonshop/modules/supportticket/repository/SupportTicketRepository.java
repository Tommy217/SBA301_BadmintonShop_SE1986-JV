package fu.se.badmintonshop.modules.supportticket.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import fu.se.badmintonshop.modules.supportticket.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByTicketCode(String ticketCode);

    Optional<SupportTicket> findByOrderId(Long orderId);

    List<SupportTicket> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderIdAndStatusNot(Long orderId, String status);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<String> statuses);

    boolean existsByOrderIdAndStatus(Long orderId, String status);

    long countByOrderIdAndStatusNot(Long orderId, String status);

    long countByOrderId(Long orderId);

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT e FROM SupportTicket e WHERE e.id=?1")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SupportTicket> findLockedById(Long id);

    @Query(
            "SELECT COUNT(t) FROM SupportTicket t WHERE t.orderId=?1 AND t.status IN ('PENDING','open','under_review','AWAITING_RETURN','PENDING_APPROVAL','PENDING_TRANSFER','REFUNDED')")
    long countCancellationBlockers(Long orderId);

    List<SupportTicket> findByOrderIdInOrderById(Collection<Long> orderIds);
}
