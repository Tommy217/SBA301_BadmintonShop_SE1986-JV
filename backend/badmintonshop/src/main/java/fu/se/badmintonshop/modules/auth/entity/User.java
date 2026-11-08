package fu.se.badmintonshop.modules.auth.entity;

import fu.se.badmintonshop.modules.auth.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 190)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Nationalized
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public String getRoleCode() {
        if (this.role == null) {
            throw new IllegalStateException("User ID " + this.id + " has no role assigned");
        }
        return this.role.getCode();
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    public boolean isLocked() {
        return "locked".equalsIgnoreCase(status);
    }

    public UserStatus getStatusEnum() {
        return UserStatus.fromString(status);
    }

    public void setStatusEnum(UserStatus userStatus) {
        if (userStatus == null) {
            throw new IllegalArgumentException("UserStatus cannot be null");
        }
        this.status = userStatus.getValue();
    }
}
