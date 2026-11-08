package fu.se.badmintonshop.modules.supportticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import fu.se.badmintonshop.modules.supportticket.entity.TicketEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketEvidenceRepository extends JpaRepository<TicketEvidence, Long> {
    List<TicketEvidence> findByTicketId(Long ticketId);

    @Query("SELECT e FROM TicketEvidence e WHERE e.ticketId=?1 ORDER BY e.id")
    List<TicketEvidence> findTicketDetails(Long ticketId);
}
