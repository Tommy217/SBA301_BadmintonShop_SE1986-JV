package fu.se.badmintonshop.modules.invoice.repository;

import fu.se.badmintonshop.modules.invoice.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    List<InvoiceItem> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
