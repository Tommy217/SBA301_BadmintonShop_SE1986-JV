package fu.se.badmintonshop.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Nationalized
    @Column(name = "receiver_name", nullable = false, length = 150)
    private String receiverName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Nationalized
    @Column(name = "province", nullable = false, length = 100)
    private String province;

    @Nationalized
    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Nationalized
    @Column(name = "ward", length = 100)
    private String ward;

    @Nationalized
    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
}

