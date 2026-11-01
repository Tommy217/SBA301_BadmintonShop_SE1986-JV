package fu.se.badmintonshop.modules.support_ticket.repository;

import fu.se.badmintonshop.modules.support_ticket.entity.TicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {
    List<TicketStatusHistory> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}

