package fu.se.badmintonshop.modules.shop;

import fu.se.badmintonshop.modules.auth.AuthService;
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
        return authentication != null && authentication.getPrincipal() instanceof AuthService.User user ? user : null;
    }
}
