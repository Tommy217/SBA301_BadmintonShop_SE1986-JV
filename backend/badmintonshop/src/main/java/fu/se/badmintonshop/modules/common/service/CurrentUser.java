package fu.se.badmintonshop.modules.common.service;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import org.springframework.security.core.Authentication;

public final class CurrentUser {
    private CurrentUser() {}

    public static CustomUserDetails from(Authentication authentication) {
        if (authentication == null) return null;
        if (authentication.getPrincipal() instanceof CustomUserDetails user) {
            return user;
        }
        return null;
    }
}
