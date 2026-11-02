package fu.se.badmintonshop.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "apparel_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApparelSpec {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "apparel_kind", nullable = false, length = 20)
    private String apparelKind;

    @Nationalized
    @Column(name = "material", length = 150)
    private String material;
}
