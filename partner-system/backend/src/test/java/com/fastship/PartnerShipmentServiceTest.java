package com.fastship;

import com.fastship.document.PartnerShipment;
import com.fastship.dto.ApproveRequest;
import com.fastship.dto.ShipmentRequestBody;
import com.fastship.dto.StatusUpdateRequest;
import com.fastship.exception.PartnerException;
import com.fastship.repository.PartnerShipmentRepository;
import com.fastship.document.TrackingEvent;
import com.fastship.service.PartnerShipmentService;
import com.fastship.service.WebhookOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PartnerShipmentServiceTest {

    PartnerShipmentRepository repo;
    WebhookOutboxService outbox;
    PartnerShipmentService service;

    @BeforeEach
    void setUp() {
        repo = mock(PartnerShipmentRepository.class);
        outbox = mock(WebhookOutboxService.class);
        service = new PartnerShipmentService(repo, outbox, "badminton-shop", "http://localhost:8082/track");
        when(repo.save(any(PartnerShipment.class))).thenAnswer(inv -> {
            PartnerShipment s = inv.getArgument(0);
            if (s.getId() == null) s.setId("ref-1");
            return s;
        });
    }

    private ShipmentRequestBody body() {
        return new ShipmentRequestBody(5L, "10", "BS-10", "Nguyễn Văn A", "0900000000",
                "12 Lê Lợi, Hà Nội", "fast", 1600, null, "dễ vỡ");
    }

    private PartnerShipment withStatus(String status) {
        PartnerShipment s = new PartnerShipment();
        s.setId("ref-1");
        s.setStatus(status);
        s.setServiceType("standard");
        s.setShippingAddress("Hà Nội");
        when(repo.findById("ref-1")).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void submitCreatesRequestedShipmentWithFee() {
        when(repo.findByShopCodeAndShopShipmentId("badminton-shop", 5L)).thenReturn(Optional.empty());

        PartnerShipment s = service.submit(body());

        assertEquals("ref-1", s.getId());
        assertEquals("requested", s.getStatus());
        assertNull(s.getTrackingCode(), "chua duyet thi chua co ma");
        assertEquals(new BigDecimal("55000"), s.getShippingFee(), "fast 45.000 + 2 nac 500g vuot 1kg");
        assertEquals(1, s.getEvents().size());
        verifyNoInteractions(outbox);
    }

    @Test
    void submitIsIdempotentPerShopShipment() {
        PartnerShipment existing = new PartnerShipment();
        existing.setId("ref-old");
        when(repo.findByShopCodeAndShopShipmentId("badminton-shop", 5L)).thenReturn(Optional.of(existing));

        assertSame(existing, service.submit(body()));
        verify(repo, never()).save(any());
    }

    @Test
    void submitRaceReturnsSavedDocument() {
        PartnerShipment saved = new PartnerShipment();
        when(repo.findByShopCodeAndShopShipmentId("badminton-shop", 5L))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(saved));
        when(repo.save(any(PartnerShipment.class))).thenThrow(new DuplicateKeyException("dup"));

        assertSame(saved, service.submit(body()));
    }

    @Test
    void approveAssignsTrackingCodeAndShipper() {
        withStatus("requested");
        when(repo.existsByTrackingCode(anyString())).thenReturn(false);

        PartnerShipment s = service.approve("ref-1", new ApproveRequest(null, 2, null, null));

        assertEquals("pending_pickup", s.getStatus());
        assertTrue(s.getTrackingCode().matches("FS-\\d{6}"));
        assertEquals("http://localhost:8082/track?code=" + s.getTrackingCode(), s.getTrackingUrl());
        assertNotNull(s.getShipperName());
        assertNotNull(s.getEstimatedDeliveryDate());
        // webhook bao shop gan voi moc hanh trinh vua them
        verify(outbox).enqueue(same(s), argThat((TrackingEvent e) -> "pending_pickup".equals(e.getStatus())));
    }

    @Test
    void approveTwiceIsConflict() {
        withStatus("pending_pickup");
        PartnerException e = assertThrows(PartnerException.class, () -> service.approve("ref-1", null));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verifyNoInteractions(outbox);
    }

    @Test
    void rejectStoresReason() {
        withStatus("requested");
        PartnerShipment s = service.reject("ref-1", "Ngoài vùng phục vụ");
        assertEquals("rejected", s.getStatus());
        assertEquals("Ngoài vùng phục vụ", s.getRejectReason());
    }

    @Test
    void courierUpdatesFollowTransitions() {
        withStatus("pending_pickup");
        PartnerShipment s = service.updateStatus("ref-1", new StatusUpdateRequest("in_transit", null, null));
        assertEquals("in_transit", s.getStatus());
        assertNotNull(s.getShippedAt());

        PartnerException skip = assertThrows(PartnerException.class,
                () -> service.updateStatus("ref-1", new StatusUpdateRequest("delivered", null, null)));
        assertEquals(HttpStatus.CONFLICT, skip.getStatus(), "in_transit -> delivered phai qua out_for_delivery");

        PartnerException approveViaStatus = assertThrows(PartnerException.class,
                () -> service.updateStatus("ref-1", new StatusUpdateRequest("pending_pickup", null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, approveViaStatus.getStatus());
    }

    @Test
    void shopCancelOnlyBeforePickupAndIsIdempotent() {
        PartnerShipment s = withStatus("pending_pickup");
        assertEquals("cancelled", service.cancelByShop("ref-1", "Khách đổi ý").getStatus());
        assertEquals("cancelled", service.cancelByShop("ref-1", "lần 2").getStatus());
        assertEquals("Khách đổi ý", s.getCancelReason());

        withStatus("in_transit");
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(PartnerException.class, () -> service.cancelByShop("ref-1", "x")).getStatus());
    }

    @Test
    void unknownShipmentIs404() {
        when(repo.findById("nope")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(PartnerException.class, () -> service.get("nope")).getStatus());
    }
}
