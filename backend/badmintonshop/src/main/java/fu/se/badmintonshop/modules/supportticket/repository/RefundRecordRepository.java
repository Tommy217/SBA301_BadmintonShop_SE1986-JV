package fu.se.badmintonshop.modules.supportticket.repository;

import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.Query;

import fu.se.badmintonshop.modules.supportticket.entity.RefundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRecordRepository extends JpaRepository<RefundRecord, Long> {
    Optional<RefundRecord> findByTicketId(Long ticketId);

    Optional<RefundRecord> findByBankReference(String bankReference);

    boolean existsByBankReference(String bankReference);

    List<RefundRecord> findByTicketIdInOrderById(Collection<Long> ticketIds);

    @Query("SELECT e FROM RefundRecord e WHERE e.ticketId=?1 ORDER BY e.id")
    List<RefundRecord> findTicketDetails(Long ticketId);
}
