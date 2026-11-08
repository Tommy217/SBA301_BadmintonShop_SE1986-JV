package fu.se.badmintonshop.modules.supportticket.repository;

import fu.se.badmintonshop.modules.supportticket.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByTicketCode(String ticketCode);

    Optional<SupportTicket> findByOrderId(Long orderId);

    List<SupportTicket> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderIdAndStatusNot(Long orderId, String status);

    boolean existsByOrderIdAndStatusIn(Long orderId, java.util.Collection<String> statuses);

    boolean existsByOrderIdAndStatus(Long orderId, String status);

    long countByOrderIdAndStatusNot(Long orderId, String status);

    long countByOrderId(Long orderId);

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(String status);
}

