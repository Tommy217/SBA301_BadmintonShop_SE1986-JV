package fu.se.badmintonshop.modules.ticket.repository;

import fu.se.badmintonshop.modules.ticket.entity.SupportTicketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketItemRepository extends JpaRepository<SupportTicketItem, Long> {
    List<SupportTicketItem> findByTicketId(Long ticketId);
}

