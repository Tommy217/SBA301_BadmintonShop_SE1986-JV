package fu.se.badmintonshop.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vouchers")
@IdClass(UserVoucher.Key.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVoucher {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "voucher_id")
    private Integer voucherId;

    @Column(name = "assigned_at", insertable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long userId;
        private Integer voucherId;
    }
}
