package fu.se.badmintonshop.modules.ticket.repository;

import fu.se.badmintonshop.modules.ticket.entity.RefundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRecordRepository extends JpaRepository<RefundRecord, Long> {
    Optional<RefundRecord> findByTicketId(Long ticketId);

    Optional<RefundRecord> findByBankReference(String bankReference);

    boolean existsByBankReference(String bankReference);
}

