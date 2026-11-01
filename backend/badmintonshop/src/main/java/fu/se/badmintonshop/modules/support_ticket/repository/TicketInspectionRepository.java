package fu.se.badmintonshop.modules.support_ticket.repository;

import fu.se.badmintonshop.modules.support_ticket.entity.TicketInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketInspectionRepository extends JpaRepository<TicketInspection, Long> {
    Optional<TicketInspection> findByTicketId(Long ticketId);
}

