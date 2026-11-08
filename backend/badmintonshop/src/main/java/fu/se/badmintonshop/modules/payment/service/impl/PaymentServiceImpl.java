package fu.se.badmintonshop.modules.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.payment.dto.request.CreatePaymentRequest;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookData;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.response.CreatePaymentResponse;
import fu.se.badmintonshop.modules.payment.dto.response.PaymentDetailResponse;
import fu.se.badmintonshop.modules.payment.entity.Payment;
import fu.se.badmintonshop.modules.payment.entity.PaymentTransaction;
import fu.se.badmintonshop.modules.payment.repository.PaymentRepository;
import fu.se.badmintonshop.modules.payment.repository.PaymentTransactionRepository;
import fu.se.badmintonshop.modules.payment.service.PayOSService;
import fu.se.badmintonshop.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final PayOSService payOSService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));

        if ("paid".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalStateException("Đơn hàng #" + order.getOrderCode() + " đã được thanh toán trước đó.");
        }
        if ("cancelled".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng #" + order.getOrderCode() + " đã bị hủy, không thể thanh toán.");
        }

        BigDecimal payAmount = order.getTotalAmount();
        if (request.getAmount() != null && request.getAmount().compareTo(payAmount) != 0) {
            throw new IllegalArgumentException("Số tiền phải khớp tổng đơn hàng trong database.");
        }
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }

        // Kiểm tra xem đơn hàng đã có phiên thanh toán pending chưa để tái sử dụng, tránh tạo bản ghi trùng lặp
        Optional<Payment> existingPendingOpt = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), "pending");
        Payment payment;
        boolean hasActiveLink = false;
        if (existingPendingOpt.isPresent()) {
            payment = existingPendingOpt.get();
            payment.setMethod("qr");
            payment.setGateway("payos");
            payment.setAmount(payAmount);
            if (payment.getQrCodeUrl() != null && payment.getCheckoutUrl() != null
                    && payment.getCreatedAt() != null
                    && payment.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(14))) {
                hasActiveLink = true;
            }
        } else {
            payment = Payment.builder()
                    .orderId(order.getId())
                    .method("qr")
                    .gateway("payos")
                    .status("pending")
                    .amount(payAmount)
                    .build();
        }
        payment = paymentRepository.save(payment);

        // Random provider code survives database identity resets without reusing old links.
        long payosOrderCode = hasActiveLink && providerOrderCode(payment) != null
                ? providerOrderCode(payment)
                : java.util.concurrent.ThreadLocalRandom.current().nextLong(100_000_000_000_000L, 999_999_999_999_999L);
        payment.setGatewayTxnRef(String.valueOf(payosOrderCode));

        String paymentCode = formatPaymentCode(payment.getId(), payment.getCreatedAt());
        String checkoutUrl = payment.getCheckoutUrl();
        String qrCodeUrl = payment.getQrCodeUrl();

        if (!hasActiveLink) {
            String description = request.getDescription() != null && !request.getDescription().isBlank()
                    ? request.getDescription()
                    : "Thanh toan #" + order.getOrderCode();

            // Gọi PayOS API để tạo link thanh toán VietQR
            Map<String, String> payosResult = payOSService.createPaymentLink(
                    payosOrderCode,
                    payAmount,
                    description,
                    request.getBuyerName() != null ? request.getBuyerName() : order.getReceiverName(),
                    request.getBuyerEmail(),
                    request.getBuyerPhone() != null ? request.getBuyerPhone() : order.getReceiverPhone(),
                    request.getReturnUrl(),
                    request.getCancelUrl()
            );

            checkoutUrl = payosResult.get("checkoutUrl");
            qrCodeUrl = payosResult.get("qrCodeUrl");
            payment.setQrCodeUrl(qrCodeUrl);
            payment.setCheckoutUrl(checkoutUrl);
            payment = paymentRepository.save(payment);

            // Ghi log giao dịch gửi request
            try {
                Map<String, Object> logPayload = Map.of(
                        "request", request,
                        "payosOrderCode", payosOrderCode,
                        "paymentCode", paymentCode,
                        "payosResult", payosResult
                );
                PaymentTransaction transaction = PaymentTransaction.builder()
                        .paymentId(payment.getId())
                        .direction("request")
                        .payload(objectMapper.writeValueAsString(logPayload))
                        .httpStatus(200)
                        .build();
                paymentTransactionRepository.save(transaction);
            } catch (Exception e) {
                log.warn("Không thể ghi log giao dịch request: {}", e.getMessage());
            }
        }

        LocalDateTime expiresAt = payment.getCreatedAt() != null
                ? payment.getCreatedAt().plusMinutes(15)
                : LocalDateTime.now().plusMinutes(15);

        return CreatePaymentResponse.builder()
                .paymentId(payment.getId())
                .paymentCode(paymentCode)
                .payosOrderCode(payosOrderCode)
                .checkoutUrl(checkoutUrl)
                .qrCodeUrl(qrCodeUrl)
                .amount(payAmount)
                .status(payment.getStatus())
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    @Transactional
    public void handlePayOSWebhook(PayOSWebhookRequest webhookRequest) {
        if (webhookRequest == null || webhookRequest.getData() == null) {
            throw new IllegalArgumentException("Payload Webhook không hợp lệ.");
        }

        PayOSWebhookData data = webhookRequest.getData();
        Long orderCode = data.getOrderCode();

        // 1. Xác thực chữ ký số HMAC-SHA256
        boolean isValidSignature = payOSService.verifyWebhookSignature(webhookRequest);
        if (!isValidSignature) {
            log.error("Xác thực chữ ký HMAC-SHA256 Webhook thất bại cho orderCode: {}", orderCode);
            throw new SecurityException("Chữ ký số Webhook không hợp lệ!");
        }

        // 2. Tìm kiếm Payment tương ứng
        Payment payment = findPaymentByOrderCodeOrReference(orderCode, data.getReference());
        if (payment == null) {
            log.error("Không tìm thấy Payment tương ứng với payOS orderCode: {}", orderCode);
            throw new IllegalArgumentException("Không tìm thấy giao dịch thanh toán cho mã: " + orderCode);
        }

        // 3. Ghi log callback vào payment_transactions
        try {
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .paymentId(payment.getId())
                    .direction("callback")
                    .payload(objectMapper.writeValueAsString(webhookRequest))
                    .httpStatus(200)
                    .build();
            paymentTransactionRepository.save(transaction);
        } catch (Exception e) {
            log.warn("Không thể lưu log callback giao dịch: {}", e.getMessage());
        }

        // 4. Cơ chế Idempotency & Cập nhật thanh toán
        markPaymentAsPaid(payment, data.getReference());
    }

    @Transactional
    public void markPaymentAsPaid(Payment payment, String reference) {
        if ("paid".equalsIgnoreCase(payment.getStatus())) {
            log.info("Payment #{} đã ở trạng thái 'paid'. Bỏ qua cập nhật trùng lặp (Idempotency).", payment.getId());
            return;
        }

        payment.setStatus("paid");
        payment.setPaidAt(LocalDateTime.now());
        // Keep the provider order code; bank references belong to transaction logs.
        paymentRepository.save(payment);

        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null) {
            order.setPaymentStatus("paid");
            order.setStatus("confirmed");
            orderRepository.save(order);
            log.info("Cập nhật thành công đơn hàng #{}: payment_status='paid', status='confirmed'", order.getOrderCode());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán ID: " + id));
        return toDetailResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentByCode(String paymentCode) {
        Long id = parsePaymentIdFromCode(paymentCode);
        if (id != null) {
            return getPaymentById(id);
        }
        Payment payment = paymentRepository.findByGatewayTxnRef(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thanh toán với mã: " + paymentCode));
        return toDetailResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentByPayosOrderCode(Long payosOrderCode) {
        Payment payment = findPaymentByOrderCodeOrReference(payosOrderCode, null);
        if (payment == null) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch thanh toán cho payosOrderCode: " + payosOrderCode);
        }
        return toDetailResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDetailResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::toDetailResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentDetailResponse cancelPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán ID: " + paymentId));

        if ("paid".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("Không thể hủy giao dịch đã thanh toán thành công.");
        }

        payment.setStatus("cancelled");
        payment.setCancelledAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Long payosOrderCode = providerOrderCode(payment);
        if (payosOrderCode != null) payOSService.cancelPaymentLink(payosOrderCode, reason);

        return toDetailResponse(payment);
    }

    @Override
    public void confirmWebhook(String webhookUrl) {
        payOSService.confirmWebhook(webhookUrl);
    }

    @Override
    @Transactional
    public int processExpiredPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Payment> expiredList = paymentRepository.findByStatusAndCreatedAtBefore("pending", cutoff);
        int count = 0;

        for (Payment payment : expiredList) {
            payment.setStatus("expired");
            payment.setCancelledAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Nhả trạng thái đơn hàng nếu chưa trả tiền
            Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
            if (order != null && "unpaid".equalsIgnoreCase(order.getPaymentStatus()) && "pending".equalsIgnoreCase(order.getStatus())) {
                order.setStatus("cancelled");
                orderRepository.save(order);
                log.info("Tự động hủy đơn hàng #{} do quá hạn thanh toán 15 phút.", order.getOrderCode());
            }
            count++;
        }
        return count;
    }

    @Override
    @Transactional
    public PaymentDetailResponse syncPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch ID: " + paymentId));
        if ("paid".equalsIgnoreCase(payment.getStatus())) {
            return toDetailResponse(payment);
        }

        Long payosOrderCode = providerOrderCode(payment);
        if (!"payos".equalsIgnoreCase(payment.getGateway()) || payment.getCheckoutUrl() == null
                || payment.getCheckoutUrl().isBlank() || payosOrderCode == null) {
            return toDetailResponse(payment);
        }
        JsonNode data = payOSService.getPaymentLinkInformation(payosOrderCode);
        if (data != null) {
            String status = data.path("status").asText();
            if ("PAID".equalsIgnoreCase(status)) {
                String ref = null;
                JsonNode txns = data.path("transactions");
                if (txns.isArray() && !txns.isEmpty()) {
                    ref = txns.get(0).path("reference").asText(null);
                }
                markPaymentAsPaid(payment, ref);
            } else if ("CANCELLED".equalsIgnoreCase(status)) {
                payment.setStatus("cancelled");
                payment.setCancelledAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }
        return toDetailResponse(payment);
    }

    @Override
    @Transactional
    public PaymentDetailResponse syncPaymentStatusByOrderId(Long orderId) {
        List<Payment> list = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (list.isEmpty()) {
            return null;
        }
        return syncPaymentStatus(list.get(0).getId());
    }

    @Override
    @Transactional
    public void syncPendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Payment> pendingList = paymentRepository.findByStatusAndCreatedAtAfter("pending", cutoff);
        for (Payment p : pendingList) {
            try {
                syncPaymentStatus(p.getId());
            } catch (Exception e) {
                log.warn("Lỗi khi đồng bộ trạng thái payment #{}: {}", p.getId(), e.getMessage());
            }
        }
    }

    private Long providerOrderCode(Payment payment) {
        try {
            return payment.getGatewayTxnRef() == null ? null : Long.valueOf(payment.getGatewayTxnRef());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Payment findPaymentByOrderCodeOrReference(Long orderCode, String reference) {
        if (orderCode == null) return null;
        return paymentRepository.findByGatewayTxnRef(String.valueOf(orderCode))
                .filter(p -> "payos".equalsIgnoreCase(p.getGateway()))
                .orElse(null);
    }

    private PaymentDetailResponse toDetailResponse(Payment payment) {
        String orderCode = null;
        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null) {
            orderCode = order.getOrderCode();
        }

        Long payosOrderCode = providerOrderCode(payment);
        String paymentCode = formatPaymentCode(payment.getId(), payment.getCreatedAt());

        return PaymentDetailResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .orderCode(orderCode)
                .paymentCode(paymentCode)
                .payosOrderCode(payosOrderCode)
                .method(payment.getMethod())
                .gateway(payment.getGateway())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .gatewayTxnRef(payment.getGatewayTxnRef())
                .qrCodeUrl(payment.getQrCodeUrl())
                .paidAt(payment.getPaidAt())
                .cancelledAt(payment.getCancelledAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private String formatPaymentCode(Long paymentId, LocalDateTime createdAt) {
        String dateStr = (createdAt != null ? createdAt : LocalDateTime.now())
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("PAY-%s-%05d", dateStr, paymentId != null ? paymentId : 0L);
    }

    private Long parsePaymentIdFromCode(String code) {
        if (code == null) return null;
        Pattern pattern = Pattern.compile("PAY-\\d{8}-(\\d+)");
        Matcher matcher = pattern.matcher(code.trim());
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
