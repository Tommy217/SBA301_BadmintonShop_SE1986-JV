package fu.se.badmintonshop.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Formula;
import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;

@Entity
@Table(name = "stock_audit_items", uniqueConstraints = @UniqueConstraint(name = "uq_audit_variant", columnNames = {"audit_id", "variant_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAuditItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private StockAudit audit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "system_qty", nullable = false)
    private Integer systemQty;

    @Column(name = "counted_qty", nullable = false)
    private Integer countedQty;

    // Mirrors the computed diff_qty column without writing it or emitting vendor-specific DDL.
    @Formula("counted_qty - system_qty")
    private Integer diffQty;

    @Nationalized
    @Column(name = "note", length = 200)
    private String note;
}
