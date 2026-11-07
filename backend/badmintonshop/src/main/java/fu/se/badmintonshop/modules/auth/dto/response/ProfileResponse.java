package fu.se.badmintonshop.modules.auth.dto.response;

import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    private List<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static ProfileResponse fromUser(User user, List<String> roles) {
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
            .roles(roles)
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .build();
    }

    public static ProfileResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        List<String> roleCodes = user.getRoles() == null ? List.of() : user.getRoles().stream()
            .map(Role::getCode)
            .filter(Objects::nonNull)
            .map(String::toUpperCase)
            .sorted()
            .toList();
        return fromUser(user, roleCodes);
    }
}

