package fu.se.badmintonshop.modules.supportticket.repository;

import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.Query;

import fu.se.badmintonshop.modules.supportticket.entity.TicketInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketInspectionRepository extends JpaRepository<TicketInspection, Long> {
    Optional<TicketInspection> findByTicketId(Long ticketId);

    List<TicketInspection> findByTicketIdInOrderById(Collection<Long> ticketIds);

    @Query("SELECT e FROM TicketInspection e WHERE e.ticketId=?1 ORDER BY e.id")
    List<TicketInspection> findTicketDetails(Long ticketId);
}
