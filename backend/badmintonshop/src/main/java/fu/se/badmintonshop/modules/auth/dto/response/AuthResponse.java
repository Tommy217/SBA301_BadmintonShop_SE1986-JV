package fu.se.badmintonshop.modules.auth.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long id;
    private String email;
    private String fullName;
    private List<String> roles;
    private String authToken;   // For frontend compatibility
    private String accessToken; // Standard JWT
    private String tokenType;   // "Bearer"
    private Long expiresIn;     // in seconds
    private UserResponse user;
}

