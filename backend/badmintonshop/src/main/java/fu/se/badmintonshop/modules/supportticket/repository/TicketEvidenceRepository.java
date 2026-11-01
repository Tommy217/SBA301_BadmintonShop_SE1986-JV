package fu.se.badmintonshop.modules.supportticket.repository;

import fu.se.badmintonshop.modules.supportticket.entity.TicketEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketEvidenceRepository extends JpaRepository<TicketEvidence, Long> {
    List<TicketEvidence> findByTicketId(Long ticketId);
}

