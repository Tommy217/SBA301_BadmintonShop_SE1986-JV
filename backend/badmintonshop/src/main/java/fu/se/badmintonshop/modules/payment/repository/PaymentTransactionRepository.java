package fu.se.badmintonshop.modules.payment.repository;

import fu.se.badmintonshop.modules.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    Optional<PaymentTransaction> findFirstByPaymentIdAndDirectionOrderByCreatedAtDesc(Long paymentId, String direction);

    Optional<PaymentTransaction> findFirstByDirectionAndPayloadContaining(String direction, String searchPattern);
}

