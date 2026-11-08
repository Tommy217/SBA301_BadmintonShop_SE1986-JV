package fu.se.badmintonshop.modules.supportticket.controller;

import fu.se.badmintonshop.modules.order.service.OrderService;
import fu.se.badmintonshop.modules.common.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/support/actions")
@RequiredArgsConstructor
public class SupportActionController {
    private final OrderService orders;
    private final fu.se.badmintonshop.modules.supportticket.service.SupportActions actions;

    @PostMapping
    public Map<String, Object> execute(
            @RequestBody Map<String, Object> action, Authentication auth) {
        actions.execute(
                fu.se.badmintonshop.modules.common.service.CommerceValidation.text(action, "type"),
                action,
                CurrentUser.from(auth));
        return orders.snapshot(CurrentUser.from(auth));
    }
}
