package fu.se.badmintonshop.modules.payment.repository;

import fu.se.badmintonshop.modules.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, String status);

    Optional<Payment> findByGatewayTxnRef(String gatewayTxnRef);

    List<Payment> findByStatusAndCreatedAtBefore(String status, LocalDateTime cutoff);
}

