package fu.se.badmintonshop.modules.delivery.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.delivery.gateway.FastShipSignature;
import fu.se.badmintonshop.modules.delivery.service.FastShipWebhookService;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

// doi tac goi vao (khong dang nhap), xac thuc bang chu ky HMAC; da mo permitAll trong SecurityConfig
@RestController
@RequestMapping("/api/webhooks/carriers")
@RequiredArgsConstructor
public class CarrierWebhookController {

    private final FastShipWebhookService fastShipWebhookService;

    // nhan byte[] de kiem chu ky tren dung body goc
    @PostMapping("/fastship")
    public ApiResponse<Map<String, String>> fastship(
            @RequestBody byte[] body,
            @RequestHeader(value = FastShipSignature.HEADER, required = false) String signature) {
        ShipmentService.WebhookResult result = fastShipWebhookService.handle(body, signature);
        String message = result == ShipmentService.WebhookResult.APPLIED
                ? "Đã cập nhật vận đơn."
                : "Đã nhận, không cần cập nhật (" + result.name().toLowerCase(Locale.ROOT) + ").";
        return ApiResponse.success(message, Map.of("result", result.name()));
    }
}
