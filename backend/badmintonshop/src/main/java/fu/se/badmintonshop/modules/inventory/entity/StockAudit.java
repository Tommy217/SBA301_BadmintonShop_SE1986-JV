package fu.se.badmintonshop.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_audits", uniqueConstraints = @UniqueConstraint(columnNames = "audit_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_code", nullable = false, length = 30)
    private String auditCode;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "draft";

    @Column(name = "conducted_by")
    private Long conductedBy;

    @Nationalized
    @Column(name = "note", length = 300)
    private String note;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime startedAt;

    @Column(name = "completed_at", columnDefinition = "datetime2(0)")
    private LocalDateTime completedAt;

    @Builder.Default
    @OneToMany(mappedBy = "audit", fetch = FetchType.LAZY)
    private List<StockAuditItem> items = new ArrayList<>();
}
