package fu.se.badmintonshop.modules.invoice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private String channel = "email";

    @Column(name = "recipient", length = 190)
    private String recipient;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "queued";

    @Column(name = "sent_at", columnDefinition = "datetime2(0)")
    private LocalDateTime sentAt;
}
