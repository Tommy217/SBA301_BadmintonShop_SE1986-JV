package fu.se.badmintonshop.modules.auth.dto.response;

import fu.se.badmintonshop.modules.auth.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate birthDate;
    private String avatarUrl;
    private String status;
    private boolean active;
    private boolean locked;
    private String role;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static ProfileResponse fromUser(User user, String role) {
        if (user == null) {
            return null;
        }
        return ProfileResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .gender(user.getGender())
            .birthDate(user.getBirthDate())
            .avatarUrl(user.getAvatarUrl())
            .status(user.getStatus())
            .active(user.isActive())
            .locked(user.isLocked())
            .role(role)
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .build();
    }

    public static ProfileResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        if (user.getRole() == null || user.getRole().getCode() == null) {
            throw new IllegalStateException("User ID " + user.getId() + " is missing role data");
        }
        return fromUser(user, user.getRole().getCode().toUpperCase(Locale.ROOT));
    }
}
