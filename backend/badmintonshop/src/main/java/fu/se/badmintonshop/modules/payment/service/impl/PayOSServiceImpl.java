package fu.se.badmintonshop.modules.payment.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookData;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookRequest;
import fu.se.badmintonshop.modules.payment.service.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSServiceImpl implements PayOSService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    private static final String PAYOS_API_URL = "https://api-merchant.payos.vn/v2/payment-requests";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public Map<String, String> createPaymentLink(
            Long orderCode,
            BigDecimal amount,
            String description,
            String buyerName,
            String buyerEmail,
            String buyerPhone,
            String returnUrl,
            String cancelUrl
    ) {
        Map<String, String> result = new HashMap<>();
        long intAmount = amount != null ? amount.longValue() : 0L;
        String desc = sanitizeDescription(description != null ? description : ("Don hang " + orderCode));
        String retUrl = returnUrl != null && !returnUrl.isBlank() ? returnUrl : "http://localhost:5173/payments/result?order=" + orderCode;
        String canUrl = cancelUrl != null && !cancelUrl.isBlank() ? cancelUrl : "http://localhost:5173/payments?order=" + orderCode;

        // A missing configuration must never produce a fake payment link.
        if (!isConfigured()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình payOS. Vui lòng cấu hình PAYOS_CLIENT_ID, PAYOS_API_KEY và PAYOS_CHECKSUM_KEY trên backend.");
        }

        try {
            // Chữ ký thanh toán request payOS: amount, cancelUrl, description, orderCode, returnUrl (theo thứ tự a-z)
            String signData = String.format(
                    "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    intAmount, canUrl, desc, orderCode, retUrl
            );
            String signature = calculateHmacSha256(signData, checksumKey);

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("orderCode", orderCode);
            reqBody.put("amount", intAmount);
            reqBody.put("description", desc);
            reqBody.put("returnUrl", retUrl);
            reqBody.put("cancelUrl", canUrl);
            if (buyerName != null) reqBody.put("buyerName", buyerName);
            if (buyerEmail != null) reqBody.put("buyerEmail", buyerEmail);
            if (buyerPhone != null) reqBody.put("buyerPhone", buyerPhone);
            reqBody.put("signature", signature);

            String jsonPayload = objectMapper.writeValueAsString(reqBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PAYOS_API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", clientId.trim())
                    .header("x-api-key", apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if ("00".equals(root.path("code").asText())) {
                    JsonNode dataNode = root.path("data");
                    String checkoutUrl = dataNode.path("checkoutUrl").asText();
                    String qrCode = dataNode.path("qrCode").asText();
                    String paymentLinkId = dataNode.path("paymentLinkId").asText();

                    String qrCodeUrl;
                    if (qrCode != null && !qrCode.isBlank()) {
                        qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" +
                                URLEncoder.encode(qrCode, StandardCharsets.UTF_8);
                    } else throw new IllegalStateException("payOS chưa trả mã QR thanh toán.");

                    result.put("checkoutUrl", checkoutUrl);
                    result.put("qrCodeUrl", qrCodeUrl);
                    result.put("paymentLinkId", paymentLinkId);
                    return result;
                } else {
                    log.error("Lỗi từ payOS API: code={}, desc={}", root.path("code").asText(), root.path("desc").asText());
                    throw new RuntimeException("Lỗi tạo link thanh toán payOS: " + root.path("desc").asText());
                }
            } else {
                log.error("payOS API trả về status: {}, body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Không thể kết nối tới payOS API (HTTP " + response.statusCode() + ")");
            }

        } catch (Exception e) {
            log.error("Ngoại lệ khi gọi payOS API: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Không tạo được phiên thanh toán payOS. Vui lòng thử lại.");
        }
    }

    @Override
    public boolean verifyWebhookSignature(PayOSWebhookRequest webhookRequest) {
        if (webhookRequest == null || webhookRequest.getData() == null) {
            return false;
        }

        // Refuse callbacks when signature verification is not configured.
        if (checksumKey == null || checksumKey.isBlank() || checksumKey.contains("here") || checksumKey.equals("mock")) {
            return false;
        }

        try {
            PayOSWebhookData data = webhookRequest.getData();
            Map<String, Object> dataMap = objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {});

            // Sắp xếp các key theo bảng chữ cái A-Z (canonical sorting)
            Map<String, Object> sortedMap = new TreeMap<>();
            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                if (entry.getValue() != null && !String.valueOf(entry.getValue()).isBlank()) {
                    sortedMap.put(entry.getKey(), entry.getValue());
                }
            }

            StringBuilder signData = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
                if (signData.length() > 0) {
                    signData.append("&");
                }
                signData.append(entry.getKey()).append("=").append(entry.getValue());
            }

            String calculatedSignature = calculateHmacSha256(signData.toString(), checksumKey.trim());
            boolean match = calculatedSignature.equalsIgnoreCase(webhookRequest.getSignature());
            if (!match) {
                log.warn("Webhook Signature Mismatch! Nhận: {}, Tính được: {}, chuỗi băm: {}",
                        webhookRequest.getSignature(), calculatedSignature, signData);
            }
            return match;

        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra chữ ký Webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void cancelPaymentLink(Long orderCode, String cancellationReason) {
        if (!isConfigured() || orderCode == null) {
            return;
        }
        try {
            String url = PAYOS_API_URL + "/" + orderCode + "/cancel";
            Map<String, String> body = new HashMap<>();
            body.put("cancellationReason", cancellationReason != null ? cancellationReason : "Huy boi nguoi dung");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", clientId.trim())
                    .header("x-api-key", apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("Không thể hủy payment link trên payOS: {}", e.getMessage());
        }
    }

    @Override
    public void confirmWebhook(String webhookUrl) {
        if (!isConfigured() || webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            String url = PAYOS_API_URL + "/confirm-webhook";
            Map<String, String> body = Map.of("webhookUrl", webhookUrl.trim());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", clientId.trim())
                    .header("x-api-key", apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Xác thực Webhook URL với payOS: code={}, body={}", response.statusCode(), response.body());
        } catch (Exception e) {
            log.warn("Không thể xác nhận Webhook URL với payOS: {}", e.getMessage());
        }
    }

    @Override
    public JsonNode getPaymentLinkInformation(Long orderCode) {
        if (!isConfigured() || orderCode == null) {
            return null;
        }
        try {
            String url = PAYOS_API_URL + "/" + orderCode;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-client-id", clientId.trim())
                    .header("x-api-key", apiKey.trim())
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if ("00".equals(root.path("code").asText())) {
                    return root.path("data");
                }
            } else {
                log.warn("Lỗi khi tra cứu payment link payOS cho orderCode {}: HTTP {} - {}", orderCode, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Không thể tra cứu thông tin payment link từ payOS cho orderCode {}: {}", orderCode, e.getMessage());
        }
        return null;
    }

    private boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && !clientId.contains("here") &&
                apiKey != null && !apiKey.isBlank() && !apiKey.contains("here") &&
                checksumKey != null && !checksumKey.isBlank() && !checksumKey.contains("here");
    }

    private String calculateHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính mã băm HMAC-SHA256: " + e.getMessage(), e);
        }
    }

    /**
     * payOS quy định độ dài description tối đa 25 ký tự không dấu cho VietQR.
     */
    private String sanitizeDescription(String input) {
        if (input == null) return "Thanh toan";
        String normalized = removeAccents(input);
        if (normalized.length() > 25) {
            return normalized.substring(0, 25);
        }
        return normalized;
    }

    private String removeAccents(String text) {
        String nfd = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replaceAll("đ", "d").replaceAll("Đ", "D");
    }
}
