package fu.se.badmintonshop.modules.supportticket.repository;

import java.util.List;
import java.util.Collection;

import fu.se.badmintonshop.modules.supportticket.entity.TicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {
    List<TicketStatusHistory> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<TicketStatusHistory> findByTicketIdInOrderById(Collection<Long> ticketIds);
}
