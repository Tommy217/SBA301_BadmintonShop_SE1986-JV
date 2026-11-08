package fu.se.badmintonshop.modules.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String authToken;   // For frontend compatibility
    private String accessToken; // Standard JWT
    private String tokenType;   // "Bearer"
    private Long expiresIn;     // in seconds
    private UserResponse user;
}
