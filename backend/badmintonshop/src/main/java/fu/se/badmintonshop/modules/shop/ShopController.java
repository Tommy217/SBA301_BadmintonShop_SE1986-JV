package fu.se.badmintonshop.modules.shop;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {
    private final ShopService shop;

    @GetMapping
    public Map<String, Object> snapshot(Authentication authentication) {
        return shop.snapshot(identity(authentication));
    }

    @PostMapping
    public Map<String, Object> action(@RequestBody Map<String, Object> request, Authentication authentication) {
        shop.execute(request, identity(authentication));
        return shop.snapshot(identity(authentication));
    }

    private AuthService.User identity(Authentication authentication) {
        if (authentication == null) return null;
        if (authentication.getPrincipal() instanceof AuthService.User user) return user;
        if (authentication.getPrincipal() instanceof CustomUserDetails cud) {
            return new AuthService.User(cud.getId(), cud.getEmail(), cud.getFullName(), cud.roles(), cud.isActive());
        }
        return null;
    }
}
