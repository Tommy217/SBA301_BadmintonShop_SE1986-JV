package com.fastship.controller;

import com.fastship.document.WebhookOutbox;
import com.fastship.dto.ApiResponse;
import com.fastship.service.WebhookOutboxService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// man "Nhat ky webhook" cua nhan vien FastShip (demo, chua dang nhap)
@RestController
@RequestMapping("/api/v1/console/webhooks")
public class WebhookConsoleController {

    private final WebhookOutboxService outbox;

    public WebhookConsoleController(WebhookOutboxService outbox) {
        this.outbox = outbox;
    }

    @GetMapping
    public ApiResponse<List<WebhookOutbox>> list(@RequestParam(required = false) String shipmentId,
                                                 @RequestParam(required = false) String state) {
        return ApiResponse.ok(outbox.list(shipmentId, state));
    }

    // dia chi shop dang nhan webhook (khong tra secret)
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", outbox.enabled());
        data.put("url", outbox.url());
        return ApiResponse.ok(data);
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<WebhookOutbox> retry(@PathVariable String id) {
        WebhookOutbox o = outbox.retry(id);
        String message = WebhookOutbox.SENT.equals(o.getState())
                ? "Đã gửi lại webhook cho shop."
                : "Gửi lại chưa thành công: " + o.getLastError();
        return ApiResponse.ok(message, o);
    }
}
