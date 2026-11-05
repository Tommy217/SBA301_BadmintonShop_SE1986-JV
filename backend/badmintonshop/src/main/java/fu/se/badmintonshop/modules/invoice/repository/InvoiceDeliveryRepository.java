package fu.se.badmintonshop.modules.invoice.repository;

import fu.se.badmintonshop.modules.invoice.entity.InvoiceDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceDeliveryRepository extends JpaRepository<InvoiceDelivery, Long> {
    List<InvoiceDelivery> findByInvoiceIdOrderByIdDesc(Long invoiceId);
}
