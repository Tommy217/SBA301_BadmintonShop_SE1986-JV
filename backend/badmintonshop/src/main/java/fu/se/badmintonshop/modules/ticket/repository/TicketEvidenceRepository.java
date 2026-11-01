package fu.se.badmintonshop.modules.ticket.repository;

import fu.se.badmintonshop.modules.ticket.entity.TicketEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketEvidenceRepository extends JpaRepository<TicketEvidence, Long> {
    List<TicketEvidence> findByTicketId(Long ticketId);
}

