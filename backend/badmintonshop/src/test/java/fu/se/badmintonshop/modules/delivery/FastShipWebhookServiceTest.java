package fu.se.badmintonshop.modules.delivery;

import fu.se.badmintonshop.modules.delivery.controller.CarrierWebhookController;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryExceptionHandler;
import fu.se.badmintonshop.modules.delivery.gateway.CarrierTracking;
import fu.se.badmintonshop.modules.delivery.gateway.FastShipSignature;
import fu.se.badmintonshop.modules.delivery.service.FastShipWebhookService;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FastShipWebhookServiceTest {

    static final String SECRET = "test-secret";
    static final String BODY = """
            {"eventId":"evt-9","type":"shipment.updated","shopShipmentId":5,"partnerRef":"ref-1","status":"in_transit",
             "version":3,"data":{"id":"ref-1","trackingCode":"FS-123456","status":"in_transit","statusText":"Đang trung chuyển",
             "shippingFee":32000,"version":3,"shippedAt":"2026-11-08T09:15:30.123",
             "events":[{"id":"evt-9","status":"in_transit","title":"Bưu tá đã lấy hàng","occurredAt":"2026-11-08T09:15:30"}]}}
            """;

    ShipmentService shipmentService;
    FastShipWebhookService service;

    @BeforeEach
    void setUp() {
        shipmentService = mock(ShipmentService.class);
        service = new FastShipWebhookService(shipmentService, SECRET);
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void signatureMatchesKnownHmacSha256Vector() {
        // vector chuan cua HMAC-SHA256
        byte[] msg = bytes("The quick brown fox jumps over the lazy dog");
        String expected = "sha256=f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8";
        assertEquals(expected, FastShipSignature.sign("key", msg));
        assertTrue(FastShipSignature.verify("key", msg, expected));
        assertTrue(FastShipSignature.verify("key", msg, expected.substring(7).toUpperCase()), "chap nhan hex khong tien to");
        assertFalse(FastShipSignature.verify("key", bytes("The quick brown fox jumps over the lazy cat"), expected));
        assertFalse(FastShipSignature.verify("key", msg, null));
        assertFalse(FastShipSignature.verify("", msg, expected), "chua cau hinh secret thi tu choi");
    }

    @Test
    void validWebhookIsParsedAndApplied() {
        when(shipmentService.applyCarrierWebhook(eq(5L), eq("ref-1"), eq("FS-ref-1-evt-9"), any()))
                .thenReturn(ShipmentService.WebhookResult.APPLIED);

        byte[] body = bytes(BODY);
        assertEquals(ShipmentService.WebhookResult.APPLIED, service.handle(body, FastShipSignature.sign(SECRET, body)));

        ArgumentCaptor<CarrierTracking> captor = ArgumentCaptor.forClass(CarrierTracking.class);
        verify(shipmentService).applyCarrierWebhook(eq(5L), eq("ref-1"), eq("FS-ref-1-evt-9"), captor.capture());
        CarrierTracking t = captor.getValue();
        assertEquals("in_transit", t.status());
        assertEquals("FS-123456", t.trackingCode());
        assertEquals(new BigDecimal("32000"), t.shippingFee());
        assertEquals(3L, t.version());
        assertNotNull(t.shippedAt());
        assertEquals("FS-ref-1-evt-9", t.events().get(0).externalId());
    }

    @Test
    void wrongOrMissingSignatureIsRejectedBeforeReadingBody() {
        byte[] body = bytes(BODY);
        String signedOther = FastShipSignature.sign("other-secret", body);

        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(DeliveryException.class, () -> service.handle(body, signedOther)).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(DeliveryException.class, () -> service.handle(body, null)).getStatus());
        // sua body sau khi ky
        byte[] tampered = bytes(BODY.replace("in_transit", "delivered"));
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(DeliveryException.class, () -> service.handle(tampered, FastShipSignature.sign(SECRET, body))).getStatus());
        verifyNoInteractions(shipmentService);
    }

    @Test
    void malformedPayloadIsBadRequest() {
        byte[] notJson = bytes("not json");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(DeliveryException.class, () -> service.handle(notJson, FastShipSignature.sign(SECRET, notJson))).getStatus());
        byte[] noShipment = bytes("{\"eventId\":\"e\",\"data\":{\"id\":\"ref-1\"}}");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(DeliveryException.class, () -> service.handle(noShipment, FastShipSignature.sign(SECRET, noShipment))).getStatus());
        verifyNoInteractions(shipmentService);
    }

    @Test
    void controllerReturns200ForHandledAnd401ForBadSignature() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CarrierWebhookController(service))
                .setControllerAdvice(new DeliveryExceptionHandler())
                .build();
        when(shipmentService.applyCarrierWebhook(anyLong(), anyString(), anyString(), any()))
                .thenReturn(ShipmentService.WebhookResult.DUPLICATE);
        byte[] body = bytes(BODY);

        mvc.perform(post("/api/webhooks/carriers/fastship").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(FastShipSignature.HEADER, FastShipSignature.sign(SECRET, body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("DUPLICATE"));

        mvc.perform(post("/api/webhooks/carriers/fastship").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(FastShipSignature.HEADER, "sha256=00"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
