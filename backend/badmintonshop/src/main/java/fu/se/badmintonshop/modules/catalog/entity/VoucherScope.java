package fu.se.badmintonshop.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voucher_scopes", uniqueConstraints = @UniqueConstraint(name = "uq_voucher_scope", columnNames = {"voucher_id", "scope_type", "scope_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "scope_type", nullable = false, length = 20)
    private String scopeType;

    // Polymorphic reference to a category, brand or product, selected by scopeType.
    @Column(name = "scope_id", nullable = false)
    private Long scopeId;
}
