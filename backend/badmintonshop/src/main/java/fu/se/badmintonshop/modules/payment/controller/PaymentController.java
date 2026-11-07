package fu.se.badmintonshop.modules.payment.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.payment.dto.request.CancelPaymentRequest;
import fu.se.badmintonshop.modules.payment.dto.request.ConfirmWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.request.CreatePaymentRequest;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.response.CreatePaymentResponse;
import fu.se.badmintonshop.modules.payment.dto.response.PaymentDetailResponse;
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

    /**
     * 1. Tạo phiên thanh toán VietQR (Online Checkout qua payOS)
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        log.info("Nhận yêu cầu tạo thanh toán cho orderId: {}", request.getOrderId());
        CreatePaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiên thanh toán thành công", response));
    }

    /**
     * 2. Tra cứu trạng thái thanh toán theo ID (Polling từ Frontend 3s/lần)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentById(@PathVariable Long id) {
        PaymentDetailResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 3. Tra cứu theo mã thanh toán nội bộ (PAY-yyyyMMdd-XXXXX)
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentByCode(@PathVariable String code) {
        PaymentDetailResponse response = paymentService.getPaymentByCode(code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 4. Tra cứu theo mã payosOrderCode
     */
    @GetMapping("/by-payos-code/{code}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentByPayosOrderCode(@PathVariable Long code) {
        PaymentDetailResponse response = paymentService.getPaymentByPayosOrderCode(code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 5. Lịch sử các lần thanh toán của một đơn hàng
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentDetailResponse>>> getPaymentsByOrderId(@PathVariable Long orderId) {
        List<PaymentDetailResponse> responses = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 6. Hủy phiên thanh toán
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> cancelPayment(
            @PathVariable Long id,
            @RequestBody(required = false) CancelPaymentRequest request
    ) {
        String reason = request != null ? request.getCancellationReason() : "Hủy bởi khách hàng";
        PaymentDetailResponse response = paymentService.cancelPayment(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy phiên thanh toán thành công", response));
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
}

