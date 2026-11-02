package fu.se.badmintonshop.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts", uniqueConstraints = @UniqueConstraint(columnNames = "receipt_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_code", nullable = false, length = 30)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = true)
    private Supplier supplier;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 0)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Nationalized
    @Column(name = "note", length = 300)
    private String note;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime receivedAt;

    @Builder.Default
    @OneToMany(mappedBy = "receipt", fetch = FetchType.LAZY)
    private List<GoodsReceiptItem> items = new ArrayList<>();
}
