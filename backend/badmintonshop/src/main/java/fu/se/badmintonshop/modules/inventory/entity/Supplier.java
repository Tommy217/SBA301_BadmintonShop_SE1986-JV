package fu.se.badmintonshop.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nationalized
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 190)
    private String email;

    @Nationalized
    @Column(name = "address", length = 300)
    private String address;

    @Builder.Default
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<GoodsReceipt> receipts = new ArrayList<>();
}
