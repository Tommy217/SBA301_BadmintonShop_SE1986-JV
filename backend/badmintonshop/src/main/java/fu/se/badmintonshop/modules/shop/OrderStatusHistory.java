package fu.se.badmintonshop.modules.shop;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "order_status_history") @Getter @Setter
public class OrderStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "from_status", length = 20) private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 20) private String toStatus;
    @Column(name = "changed_by") private Long changedBy;
    @Column(length = 300) private String note;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
