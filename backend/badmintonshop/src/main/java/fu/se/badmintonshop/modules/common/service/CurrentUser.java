package fu.se.badmintonshop.modules.common.service;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import org.springframework.security.core.Authentication;

public final class CurrentUser {
    private CurrentUser() {}

    public static AuthService.User from(Authentication authentication) {
        if (authentication == null) return null;
        if (authentication.getPrincipal() instanceof AuthService.User user) return user;
        if (authentication.getPrincipal() instanceof CustomUserDetails user)
            return new AuthService.User(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.role(),
                    user.isActive());
        return null;
    }
}
