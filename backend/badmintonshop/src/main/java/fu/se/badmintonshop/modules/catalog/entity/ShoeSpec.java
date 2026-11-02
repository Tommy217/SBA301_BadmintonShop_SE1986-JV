package fu.se.badmintonshop.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "shoe_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoeSpec {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Nationalized
    @Column(name = "sole_type", length = 100)
    private String soleType;

    @Nationalized
    @Column(name = "technology", length = 200)
    private String technology;

    @Column(name = "foot_width", nullable = false, length = 20)
    @Builder.Default
    private String footWidth = "regular";
}
