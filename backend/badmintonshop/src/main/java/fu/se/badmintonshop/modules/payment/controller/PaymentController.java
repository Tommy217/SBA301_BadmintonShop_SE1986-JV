package fu.se.badmintonshop.modules.payment.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.payment.dto.request.CancelPaymentRequest;
import fu.se.badmintonshop.modules.payment.dto.request.ConfirmWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.request.CreatePaymentRequest;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.response.CreatePaymentResponse;
import fu.se.badmintonshop.modules.payment.dto.response.PaymentDetailResponse;
import fu.se.badmintonshop.modules.payment.service.PaymentAccess;
import fu.se.badmintonshop.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentAccess paymentAccess;

    /**
     * 1. Tạo phiên thanh toán VietQR (Online Checkout qua payOS)
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        log.info("Nhận yêu cầu tạo thanh toán cho orderId: {}", request.getOrderId());
        paymentAccess.order(request.getOrderId());
        CreatePaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiên thanh toán thành công", response));
    }

    /**
     * 2. Tra cứu trạng thái thanh toán theo ID (Polling từ Frontend 3s/lần)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentById(@PathVariable Long id) {
        paymentAccess.payment(id);
        PaymentDetailResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 3. Tra cứu theo mã thanh toán nội bộ (PAY-yyyyMMdd-XXXXX)
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentByCode(@PathVariable String code) {
        PaymentDetailResponse response = paymentService.getPaymentByCode(code);
        paymentAccess.order(response.getOrderId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 4. Tra cứu theo mã payosOrderCode
     */
    @GetMapping("/by-payos-code/{code}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentByPayosOrderCode(@PathVariable Long code) {
        PaymentDetailResponse response = paymentService.getPaymentByPayosOrderCode(code);
        paymentAccess.order(response.getOrderId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 5. Lịch sử các lần thanh toán của một đơn hàng
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentDetailResponse>>> getPaymentsByOrderId(@PathVariable Long orderId) {
        paymentAccess.order(orderId);
        List<PaymentDetailResponse> responses = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 6. Hủy phiên thanh toán theo Payment ID
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> cancelPayment(
            @PathVariable Long id,
            @RequestBody(required = false) CancelPaymentRequest request
    ) {
        String reason = request != null && request.getCancellationReason() != null ? request.getCancellationReason() : "Hủy bởi khách hàng";
        paymentAccess.payment(id);
        PaymentDetailResponse response = paymentService.cancelPayment(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy phiên thanh toán thành công", response));
    }

    /**
     * Hủy phiên thanh toán theo Order ID
     */
    @PostMapping("/order/{orderId}/cancel")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> cancelPaymentByOrderId(
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelPaymentRequest request
    ) {
        paymentAccess.order(orderId);
        String reason = request != null && request.getCancellationReason() != null ? request.getCancellationReason() : "Hủy bởi khách hàng";
        PaymentDetailResponse response = paymentService.cancelPaymentByOrderId(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy phiên thanh toán thành công", response));
    }

    /**
     * Đồng bộ trạng thái trực tiếp từ cổng thanh toán payOS (Active Sync fallback)
     */
    @GetMapping("/{id}/sync")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> syncPayment(@PathVariable Long id) {
        paymentAccess.payment(id);
        PaymentDetailResponse response = paymentService.syncPaymentStatus(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}/sync")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> syncPaymentByOrderId(@PathVariable Long orderId) {
        paymentAccess.order(orderId);
        PaymentDetailResponse response = paymentService.syncPaymentStatusByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 7. Nhận Webhook tự động từ payOS (Server-to-Server Callback)
     */
    @PostMapping("/payos/webhook")
    public ResponseEntity<Map<String, Object>> handlePayosWebhook(
            @RequestBody PayOSWebhookRequest webhookRequest
    ) {
        log.info("Nhận Webhook từ payOS: code={}, desc={}, orderCode={}",
                webhookRequest.getCode(),
                webhookRequest.getDesc(),
                webhookRequest.getData() != null ? webhookRequest.getData().getOrderCode() : "null"
        );

        try {
            // gọi hàm handle -> xác thực chữ ký số đảm bảo tính toàn vẹn dữ liệu
            paymentService.handlePayOSWebhook(webhookRequest);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "code", "00",
                    "message", "Processed"
            ));
        } catch (SecurityException se) {
            log.error("Lỗi xác thực chữ ký Webhook payOS: {}", se.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "code", "01",
                    "message", se.getMessage()
            ));
        } catch (Exception e) {
            log.error("Lỗi xử lý Webhook payOS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "code", "99",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 8. Xác nhận Webhook URL khi đăng ký với payOS portal
     */
    @PostMapping("/payos/confirm-webhook")
    public ResponseEntity<ApiResponse<String>> confirmPayosWebhook(
            @Valid @RequestBody ConfirmWebhookRequest request
    ) {
        paymentService.confirmWebhook(request.getWebhookUrl());
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu xác thực Webhook URL tới payOS", request.getWebhookUrl()));
    }

    /**
     * 9. Tra cứu thời gian tự động hủy mã QR payment (mặc định 15 phút)
     */
    @GetMapping("/settings/timeout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentTimeout() {
        int timeoutMinutes = paymentService.getPaymentTimeoutMinutes();
        return ResponseEntity.ok(ApiResponse.success(Map.of("timeoutMinutes", timeoutMinutes)));
    }

    /**
     * 10. Quản lý cập nhật thời gian tự động hủy mã QR payment (Chỉ Manager/Admin)
     */
    @PutMapping("/settings/timeout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePaymentTimeout(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) Long managerId
    ) {
        Object minutesObj = body.get("timeoutMinutes");
        if (minutesObj == null) {
            minutesObj = body.get("timeout_minutes");
        }
        if (minutesObj == null) {
            throw new IllegalArgumentException("Vui lòng cung cấp timeoutMinutes.");
        }
        int minutes = Integer.parseInt(String.valueOf(minutesObj));
        paymentService.updatePaymentTimeoutMinutes(minutes, managerId != null ? managerId : 1L);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật thời gian hủy mã QR thành công", Map.of("timeoutMinutes", minutes)));
    }
}

