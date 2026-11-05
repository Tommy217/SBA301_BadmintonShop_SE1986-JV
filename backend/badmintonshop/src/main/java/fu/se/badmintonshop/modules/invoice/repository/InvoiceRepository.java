package fu.se.badmintonshop.modules.invoice.repository;

import fu.se.badmintonshop.modules.invoice.entity.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @EntityGraph(attributePaths = {"order", "parentInvoice"})
    List<Invoice> findAllByOrderByIssuedAtDescIdDesc();

    boolean existsByOrderIdAndInvoiceType(Long orderId, String invoiceType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findForUpdate(@Param("id") Long id);
}
