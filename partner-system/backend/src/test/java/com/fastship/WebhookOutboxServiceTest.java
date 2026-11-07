package com.fastship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fastship.document.PartnerShipment;
import com.fastship.document.TrackingEvent;
import com.fastship.document.WebhookOutbox;
import com.fastship.repository.WebhookOutboxRepository;
import com.fastship.service.WebhookOutboxService;
import com.fastship.webhook.WebhookSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookOutboxServiceTest {

    static final String URL = "http://shop.test/api/webhooks/carriers/fastship";

    WebhookOutboxRepository repo;
    ObjectMapper mapper;
    // ket qua gia cho tung lan goi shop: so = HTTP status, null = loi mang
    Deque<Integer> responses;
    WebhookOutboxService service;

    @BeforeEach
    void setUp() {
        repo = mock(WebhookOutboxRepository.class);
        when(repo.save(any(WebhookOutbox.class))).thenAnswer(inv -> inv.getArgument(0));
        mapper = new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        responses = new ArrayDeque<>();
        service = fake(URL);
    }

    private WebhookOutboxService fake(String url) {
        return new WebhookOutboxService(repo, mapper, url, "secret", 5) {
            @Override
            protected Response post(WebhookOutbox o) throws IOException {
                Integer status = responses.poll();
                if (status == null) throw new IOException("Connection refused");
                return new Response(status, status == 200 ? "{\"success\":true}" : "lỗi");
            }
        };
    }

    private PartnerShipment shipment(Long shopShipmentId) {
        PartnerShipment s = new PartnerShipment();
        s.setId("ref-1");
        s.setShopCode("badminton-shop");
        s.setShopShipmentId(shopShipmentId);
        s.setStatus("pending_pickup");
        s.setTrackingCode("FS-123456");
        s.setVersion(2L);
        s.setUpdatedAt(LocalDateTime.of(2026, 11, 8, 9, 0, 5));
        return s;
    }

    private WebhookOutbox pending() {
        WebhookOutbox o = new WebhookOutbox();
        o.setEventId("evt-1");
        o.setPayload("{}");
        o.setNextRetryAt(Instant.now());
        return o;
    }

    @Test
    void signatureMatchesKnownHmacSha256Vector() {
        assertEquals("sha256=f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                WebhookSigner.sign("key", "The quick brown fox jumps over the lazy dog"));
    }

    @Test
    void enqueueStoresSnapshotWithEventIdAndVersion() throws Exception {
        TrackingEvent event = new TrackingEvent("pending_pickup", "FastShip duyệt", "Bưu cục", null);

        service.enqueue(shipment(5L), event);

        ArgumentCaptor<WebhookOutbox> captor = ArgumentCaptor.forClass(WebhookOutbox.class);
        verify(repo).save(captor.capture());
        WebhookOutbox o = captor.getValue();
        assertEquals(WebhookOutbox.PENDING, o.getState());
        assertEquals(event.getId(), o.getEventId());
        assertNotNull(o.getNextRetryAt());

        JsonNode body = mapper.readTree(o.getPayload());
        assertEquals(event.getId(), body.path("eventId").asText());
        assertEquals(5, body.path("shopShipmentId").asLong());
        assertEquals("ref-1", body.path("data").path("id").asText());
        assertEquals(2, body.path("version").asLong());
        assertEquals("2026-11-08T09:00:05", body.path("data").path("updatedAt").asText(), "ngay dang ISO giong API GET");
    }

    @Test
    void enqueueSkipsDemoShipmentsAndDisabledWebhook() {
        service.enqueue(shipment(-1L), new TrackingEvent("in_transit", "x", "y", null));
        fake("").enqueue(shipment(5L), new TrackingEvent("in_transit", "x", "y", null));
        verify(repo, never()).save(any());
    }

    @Test
    void successfulDeliveryMarksSent() {
        responses.add(200);
        WebhookOutbox o = service.deliver(pending());

        assertEquals(WebhookOutbox.SENT, o.getState());
        assertEquals(1, o.getAttempts());
        assertEquals(200, o.getLastStatusCode());
        assertNotNull(o.getSentAt());
        assertNull(o.getNextRetryAt());
    }

    @Test
    void failuresBackOffThenStopAfterSixAttempts() {
        WebhookOutbox o = pending();
        List<Long> expectedMinutes = List.of(1L, 2L, 4L, 8L, 16L);
        for (int i = 0; i < 5; i++) {
            if (i % 2 == 0) responses.add(500); // lan le khong co phan hoi gia -> loi mang
            Instant before = Instant.now();
            service.deliver(o);
            assertEquals(WebhookOutbox.PENDING, o.getState());
            assertEquals(i + 1, o.getAttempts());
            long waited = Duration.between(before, o.getNextRetryAt()).toSeconds();
            assertTrue(Math.abs(waited - expectedMinutes.get(i) * 60) <= 2, "lan " + (i + 1) + " cho " + waited + "s");
            assertNotNull(o.getLastError());
        }
        service.deliver(o); // lan 6: loi mang
        assertEquals(WebhookOutbox.FAILED, o.getState());
        assertEquals(6, o.getAttempts());
        assertNull(o.getNextRetryAt());
    }

    @Test
    void retryResetsAttemptsAndSendsImmediately() {
        WebhookOutbox failed = pending();
        failed.setId("w-1");
        failed.setState(WebhookOutbox.FAILED);
        failed.setAttempts(6);
        when(repo.findById("w-1")).thenReturn(java.util.Optional.of(failed));
        responses.add(200);

        WebhookOutbox o = service.retry("w-1");

        assertEquals(WebhookOutbox.SENT, o.getState());
        assertEquals(1, o.getAttempts());
    }

    @Test
    void dispatchSendsOnlyDuePendingRecords() {
        WebhookOutbox a = pending();
        when(repo.findTop20ByStateAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(eq(WebhookOutbox.PENDING), any(Instant.class)))
                .thenReturn(List.of(a));
        responses.add(200);

        assertEquals(1, service.dispatchDue());
        assertEquals(WebhookOutbox.SENT, a.getState());
        assertEquals(0, fake(" ").dispatchDue(), "chua cau hinh url thi khong gui");
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
