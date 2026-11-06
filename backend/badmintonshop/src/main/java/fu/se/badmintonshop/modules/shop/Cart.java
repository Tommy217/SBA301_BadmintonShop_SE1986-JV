package fu.se.badmintonshop.modules.shop;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "carts") @Getter @Setter
public class Cart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false, unique = true) private Long userId;
    @Column(name = "voucher_id") private Long voucherId;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
