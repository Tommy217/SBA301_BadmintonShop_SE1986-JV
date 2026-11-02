package fu.se.badmintonshop.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;

@Entity
@Table(name = "inventory_movements", indexes = @Index(name = "idx_im_variant_time", columnList = "variant_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "movement_type", nullable = false, length = 20)
    private String movementType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "ref_type", length = 20)
    private String refType;

    // Polymorphic reference selected by refType; the schema has no foreign key here.
    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_by")
    private Long createdBy;

    @Nationalized
    @Column(name = "note", length = 300)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime createdAt;
}
