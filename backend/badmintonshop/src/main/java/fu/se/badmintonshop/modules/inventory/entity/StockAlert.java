package fu.se.badmintonshop.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;

@Entity
@Table(name = "stock_alerts", indexes = @Index(name = "idx_alert_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "stock_at_alert", nullable = false)
    private Integer stockAtAlert;

    @Column(name = "threshold", nullable = false)
    private Integer threshold;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "open";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at", columnDefinition = "datetime2(0)")
    private LocalDateTime resolvedAt;
}
