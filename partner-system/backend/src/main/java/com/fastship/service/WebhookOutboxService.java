package com.fastship.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastship.document.PartnerShipment;
import com.fastship.document.TrackingEvent;
import com.fastship.document.WebhookOutbox;
import com.fastship.exception.PartnerException;
import com.fastship.repository.WebhookOutboxRepository;
import com.fastship.webhook.WebhookSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbox webhook FastShip -> shop.
 * Moi lan duyet / tu choi / doi trang thai: ghi 1 ban ghi webhook_outbox (PENDING). WebhookDispatcher gui di;
 * loi thi gui lai sau 1, 2, 4, 8, 16 phut, qua 6 lan thi FAILED (nhan vien bam "Bắn lại" tren man Nhat ky webhook).
 * Body: { eventId, type, occurredAt, shopCode, shopShipmentId, partnerRef, status, version, data: {van don} }.
 */
@Service
public class WebhookOutboxService {

    static final int MAX_ATTEMPTS = 6;
    private static final Logger log = LoggerFactory.getLogger(WebhookOutboxService.class);

    private final WebhookOutboxRepository repository;
    private final ObjectMapper mapper;
    private final String url;
    private final String secret;
    private final Duration timeout;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WebhookOutboxService(WebhookOutboxRepository repository,
                                ObjectMapper mapper,
                                @Value("${fastship.webhook.url:}") String url,
                                @Value("${fastship.webhook.secret:dev-webhook-secret}") String secret,
                                @Value("${fastship.webhook.timeout-seconds:30}") long timeoutSeconds) {
        this.repository = repository;
        this.mapper = mapper;
        this.url = url == null ? "" : url.trim();
        this.secret = secret;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public boolean enabled() {
        return !url.isBlank();
    }

    public String url() {
        return url;
    }

    // ghi su kien sau khi van don da luu. Loi o day khong lam hong thao tac nghiep vu:
    // shop con nut "Dong bo" va job dong bo dinh ky de tu sua lech
    public void enqueue(PartnerShipment s, TrackingEvent event) {
        // van don mau (shopShipmentId <= 0) khong thuoc shop nao
        if (!enabled() || event == null || s.getShopShipmentId() == null || s.getShopShipmentId() <= 0) {
            return;
        }
        try {
            Instant now = Instant.now();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", event.getId());
            payload.put("type", "shipment.updated");
            payload.put("occurredAt", now.toString());
            payload.put("shopCode", s.getShopCode());
            payload.put("shopShipmentId", s.getShopShipmentId());
            payload.put("partnerRef", s.getId());
            payload.put("status", s.getStatus());
            payload.put("version", s.getVersion());
            payload.put("data", s);

            WebhookOutbox o = new WebhookOutbox();
            o.setEventId(event.getId());
            o.setShipmentId(s.getId());
            o.setShopShipmentId(s.getShopShipmentId());
            o.setTrackingCode(s.getTrackingCode());
            o.setStatus(s.getStatus());
            o.setPayload(mapper.writeValueAsString(payload));
            o.setState(WebhookOutbox.PENDING);
            o.setNextRetryAt(now);
            o.setCreatedAt(now);
            repository.save(o);
        } catch (DuplicateKeyException e) {
            log.debug("[webhook] su kien {} da co trong outbox", event.getId());
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("[webhook] khong ghi duoc outbox cho van don {}: {}", s.getId(), e.toString());
        }
    }

    // goi boi WebhookDispatcher; tra so ban ghi da xu ly
    public int dispatchDue() {
        if (!enabled()) {
            return 0;
        }
        List<WebhookOutbox> due = repository.findTop20ByStateAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                WebhookOutbox.PENDING, Instant.now());
        due.forEach(this::deliver);
        return due.size();
    }

    public WebhookOutbox deliver(WebhookOutbox o) {
        Instant now = Instant.now();
        o.setLastAttemptAt(now);
        try {
            Response response = post(o);
            o.setLastStatusCode(response.status());
            if (response.status() / 100 == 2) {
                o.setAttempts(o.getAttempts() + 1);
                o.setState(WebhookOutbox.SENT);
                o.setSentAt(now);
                o.setNextRetryAt(null);
                o.setLastError(null);
            } else {
                fail(o, "HTTP " + response.status() + (response.body() == null || response.body().isBlank() ? "" : ": " + response.body()), now);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            o.setLastStatusCode(null);
            fail(o, "Bị ngắt khi gửi", now);
        } catch (Exception e) {
            o.setLastStatusCode(null);
            fail(o, e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()), now);
        }
        return repository.save(o);
    }

    // nhan vien bam "Ban lai": dat lai so lan va gui ngay
    public WebhookOutbox retry(String id) {
        WebhookOutbox o = repository.findById(id)
                .orElseThrow(() -> PartnerException.notFound("Không tìm thấy webhook " + id));
        if (!enabled()) {
            throw PartnerException.conflict("Chưa cấu hình địa chỉ webhook của shop (SHOP_WEBHOOK_URL).");
        }
        o.setState(WebhookOutbox.PENDING);
        o.setAttempts(0);
        o.setNextRetryAt(Instant.now());
        return deliver(o);
    }

    public List<WebhookOutbox> list(String shipmentId, String state) {
        if (shipmentId != null && !shipmentId.isBlank()) {
            return repository.findTop100ByShipmentIdOrderByCreatedAtDesc(shipmentId.trim());
        }
        if (state != null && !state.isBlank()) {
            return repository.findTop100ByStateOrderByCreatedAtDesc(state.trim().toUpperCase(java.util.Locale.ROOT));
        }
        return repository.findTop100ByOrderByCreatedAtDesc();
    }

    // lan thu thu n that bai -> cho 2^(n-1) phut: 1, 2, 4, 8, 16
    static Duration backoff(int attempts) {
        return Duration.ofMinutes(1L << Math.max(0, Math.min(attempts - 1, 10)));
    }

    private void fail(WebhookOutbox o, String error, Instant now) {
        int attempts = o.getAttempts() + 1;
        o.setAttempts(attempts);
        o.setLastError(error.length() > 300 ? error.substring(0, 300) : error);
        if (attempts >= MAX_ATTEMPTS) {
            o.setState(WebhookOutbox.FAILED);
            o.setNextRetryAt(null);
            log.warn("[webhook] {} that bai {} lan, dung gui: {}", o.getEventId(), attempts, error);
        } else {
            o.setState(WebhookOutbox.PENDING);
            o.setNextRetryAt(now.plus(backoff(attempts)));
            log.info("[webhook] {} loi lan {}, gui lai luc {}: {}", o.getEventId(), attempts, o.getNextRetryAt(), error);
        }
    }

    // tach rieng de test thay bang ban gia
    protected Response post(WebhookOutbox o) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header(WebhookSigner.SIGNATURE_HEADER, WebhookSigner.sign(secret, o.getPayload()))
                .header(WebhookSigner.EVENT_HEADER, o.getEventId())
                .POST(HttpRequest.BodyPublishers.ofString(o.getPayload(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

    public record Response(int status, String body) {
    }
}
