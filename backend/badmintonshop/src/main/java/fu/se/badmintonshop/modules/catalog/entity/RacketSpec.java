package fu.se.badmintonshop.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;

@Entity
@Table(name = "racket_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RacketSpec {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "balance_type", nullable = false, length = 20)
    private String balanceType;

    @Column(name = "flex", nullable = false, length = 20)
    private String flex;

    @Column(name = "play_style", nullable = false, length = 20)
    private String playStyle;

    @Column(name = "max_tension_lbs", precision = 4, scale = 1)
    private BigDecimal maxTensionLbs;

    @Nationalized
    @Column(name = "material", length = 150)
    private String material;

    @Column(name = "shaft_diameter", precision = 3, scale = 1)
    private BigDecimal shaftDiameter;
}
