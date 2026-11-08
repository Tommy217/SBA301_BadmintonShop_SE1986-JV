package fu.se.badmintonshop.modules.supportticket.repository;

import java.util.List;
import java.util.Collection;

import fu.se.badmintonshop.modules.supportticket.entity.SupportTicketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketItemRepository extends JpaRepository<SupportTicketItem, Long> {
    List<SupportTicketItem> findByTicketId(Long ticketId);

    List<SupportTicketItem> findByTicketIdInOrderById(Collection<Long> ticketIds);
}
