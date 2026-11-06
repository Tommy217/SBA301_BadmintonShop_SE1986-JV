package fu.se.badmintonshop.modules.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_pay_status", columnList = "status"),
    @Index(name = "idx_pay_txn", columnList = "gateway_txn_ref")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "method", nullable = false, length = 20)
    @Builder.Default
    private String method = "qr";

    @Column(name = "gateway", length = 30)
    @Builder.Default
    private String gateway = "payos";

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(name = "gateway_txn_ref", length = 100)
    private String gatewayTxnRef;

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

