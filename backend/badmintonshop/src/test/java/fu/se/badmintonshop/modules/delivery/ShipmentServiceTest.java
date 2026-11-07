package fu.se.badmintonshop.modules.delivery;

import fu.se.badmintonshop.modules.delivery.dto.CancelShipmentRequest;
import fu.se.badmintonshop.modules.delivery.dto.CreateShipmentRequest;
import fu.se.badmintonshop.modules.delivery.dto.ShipmentResponse;
import fu.se.badmintonshop.modules.delivery.entity.Carrier;
import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import fu.se.badmintonshop.modules.delivery.entity.ShipmentEvent;
import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.gateway.CarrierGateway;
import fu.se.badmintonshop.modules.delivery.gateway.CarrierTracking;
import fu.se.badmintonshop.modules.delivery.repository.CarrierRepository;
import fu.se.badmintonshop.modules.delivery.repository.ShipmentRepository;
import fu.se.badmintonshop.modules.delivery.service.OrderFulfillmentService;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    ShipmentRepository shipmentRepository;
    @Mock
    CarrierRepository carrierRepository;
    @Mock
    OrderRepository orderRepository;
    @Mock
    CarrierGateway carrierGateway;
    @Mock
    OrderFulfillmentService fulfillment;
    @Mock
    EntityManager entityManager;

    @InjectMocks
    ShipmentService service;

    Order order;
    Carrier fastship;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(10L).orderCode("ORD-10").userId(7L).status("confirmed").paymentStatus("paid")
                .receiverName("Nguyễn Văn A").receiverPhone("0900000000").shippingAddress("Hà Nội")
                .subtotal(new BigDecimal("500000")).totalAmount(new BigDecimal("500000"))
                .build();
        fastship = Carrier.builder()
                .id(1).code("fastship").name("FastShip Express").isActive(true)
                .trackingUrlTemplate("http://localhost:8082/track?code={code}")
                .build();
    }

    private CreateShipmentRequest createRequest() {
        return CreateShipmentRequest.builder()
                .orderId(10L).carrierCode("FastShip").serviceType("fast").weightGrams(800).note("Hàng dễ vỡ")
                .build();
    }

    private Shipment shipmentWithStatus(ShipmentStatus status) {
        return Shipment.builder().id(5L).order(order).carrier(fastship).status(status.getValue()).build();
    }

    private void orderFound() {
        when(entityManager.find(Order.class, 10L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(order);
    }

    private void saveReturnsArgument() {
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            if (s.getId() == null) s.setId(5L);
            return s;
        });
    }

    // ---------- tao ----------

    @Test
    void createSendsToCarrierAndAppliesAcceptedStatus() {
        orderFound();
        when(shipmentRepository.existsByOrderIdAndStatusNotIn(eq(10L), anyCollection())).thenReturn(false);
        when(carrierRepository.findByCode("fastship")).thenReturn(Optional.of(fastship));
        saveReturnsArgument();
        doAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            s.setPartnerRef("FS-1");
            s.setTrackingCode("FS-1");
            s.setStatus("pending_pickup");
            return null;
        }).when(carrierGateway).requestShipment(any(Shipment.class));

        ShipmentResponse res = service.create(createRequest(), 3L);

        assertEquals("pending_pickup", res.getStatus());
        assertEquals("FS-1", res.getTrackingCode());
        assertEquals("fast", res.getServiceType());
        assertEquals(3L, res.getCreatedBy());
        assertNotNull(res.getLastSyncedAt());
        assertEquals(2, res.getEvents().size(), "1 event shop gui + 1 event doi tac tiep nhan");
        verify(fulfillment).onShipmentStatus(order, ShipmentStatus.PENDING_PICKUP, 3L);
    }

    @Test
    void createKeepsRequestedWhenCarrierIsDown() {
        orderFound();
        when(carrierRepository.findByCode("fastship")).thenReturn(Optional.of(fastship));
        saveReturnsArgument();
        doThrow(new DeliveryException(HttpStatus.BAD_GATEWAY, "down"))
                .when(carrierGateway).requestShipment(any(Shipment.class));

        ShipmentResponse res = service.create(createRequest(), 3L);

        assertEquals("requested", res.getStatus());
        assertNull(res.getPartnerRef());
        assertEquals(2, res.getEvents().size());
        assertEquals("Chưa gửi được sang đối tác", res.getEvents().get(1).getTitle());
        verifyNoInteractions(fulfillment);
    }

    @Test
    void createDefaultsToStandardService() {
        orderFound();
        when(carrierRepository.findByCode("fastship")).thenReturn(Optional.of(fastship));
        saveReturnsArgument();

        CreateShipmentRequest req = createRequest();
        req.setServiceType(null);

        assertEquals("standard", service.create(req, null).getServiceType());
    }

    @Test
    void createRejectsOrderNotReadyToShip() {
        order.setStatus("pending");
        orderFound();

        DeliveryException e = assertThrows(DeliveryException.class, () -> service.create(createRequest(), 3L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(carrierGateway);
    }

    @Test
    void createRejectsWhenOrderAlreadyHasActiveShipment() {
        orderFound();
        when(shipmentRepository.existsByOrderIdAndStatusNotIn(eq(10L), argThat(c ->
                c.containsAll(List.of("cancelled", "rejected")) && c.size() == 2))).thenReturn(true);

        DeliveryException e = assertThrows(DeliveryException.class, () -> service.create(createRequest(), 3L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveOrUnknownCarrier() {
        orderFound();
        fastship.setIsActive(false);
        when(carrierRepository.findByCode("fastship")).thenReturn(Optional.of(fastship));
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(DeliveryException.class, () -> service.create(createRequest(), 3L)).getStatus());

        when(carrierRepository.findByCode("fastship")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(DeliveryException.class, () -> service.create(createRequest(), 3L)).getStatus());
    }

    @Test
    void createFailsWhenOrderMissing() {
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(DeliveryException.class, () -> service.create(createRequest(), 3L)).getStatus());
    }

    // ---------- gui lai ----------

    @Test
    void resendOnlyForUnsentRequestedShipment() {
        Shipment unsent = shipmentWithStatus(ShipmentStatus.REQUESTED);
        when(shipmentRepository.findWithDetailsById(5L)).thenReturn(Optional.of(unsent));
        saveReturnsArgument();
        doAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            s.setPartnerRef("FS-2");
            s.setStatus("pending_pickup");
            return null;
        }).when(carrierGateway).requestShipment(unsent);

        assertEquals("pending_pickup", service.resend(5L, 3L).getStatus());

        Shipment sent = shipmentWithStatus(ShipmentStatus.PENDING_PICKUP);
        sent.setPartnerRef("FS-3");
        when(shipmentRepository.findWithDetailsById(6L)).thenReturn(Optional.of(sent));
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(DeliveryException.class, () -> service.resend(6L, 3L)).getStatus());
    }

    // ---------- huy ----------

    @Test
    void cancelBeforePickupCallsCarrierAndOrderFulfillment() {
        Shipment s = shipmentWithStatus(ShipmentStatus.PENDING_PICKUP);
        s.setPartnerRef("FS-1");
        when(shipmentRepository.findWithDetailsById(5L)).thenReturn(Optional.of(s));
        when(shipmentRepository.save(s)).thenReturn(s);

        ShipmentResponse res = service.cancel(5L, new CancelShipmentRequest("  Khách đổi ý  "), 3L);

        assertEquals("cancelled", res.getStatus());
        assertEquals("Khách đổi ý", res.getCancelReason());
        assertNotNull(res.getCancelledAt());
        assertFalse(res.getCancellable());
        verify(carrierGateway).cancelShipment(s, "Khách đổi ý");
        verify(fulfillment).onShipmentStatus(order, ShipmentStatus.CANCELLED, 3L);
    }

    @Test
    void cancelUnsentShipmentDoesNotCallCarrier() {
        Shipment s = shipmentWithStatus(ShipmentStatus.REQUESTED);
        when(shipmentRepository.findWithDetailsById(5L)).thenReturn(Optional.of(s));
        when(shipmentRepository.save(s)).thenReturn(s);

        assertEquals("cancelled", service.cancel(5L, new CancelShipmentRequest("x"), 3L).getStatus());
        verifyNoInteractions(carrierGateway);
    }

    @Test
    void cancelAfterPickupIsRejected() {
        Shipment s = shipmentWithStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findWithDetailsById(5L)).thenReturn(Optional.of(s));

        DeliveryException e = assertThrows(DeliveryException.class,
                () -> service.cancel(5L, new CancelShipmentRequest("x"), 3L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("in_transit", s.getStatus());
        verifyNoInteractions(carrierGateway, fulfillment);
    }

    // ---------- dong bo ----------

    private CarrierTracking tracking(String status, CarrierTracking.Event... events) {
        return new CarrierTracking("FS-1", status, "Đang giao", "Tuấn", "0987", null,
                "delivered".equals(status) ? LocalDateTime.of(2026, 11, 7, 10, 0) : null, List.of(events));
    }

    @Test
    void syncUpdatesStatusOrderAndSkipsKnownEvents() {
        Shipment s = shipmentWithStatus(ShipmentStatus.PENDING_PICKUP);
        s.setPartnerRef("FS-1");
        s.setTrackingCode("FS-1");
        s.addEvent(ShipmentEvent.builder().externalEventId("FS-FS-1-1").status("pending_pickup").source("sync")
                .occurredAt(LocalDateTime.now()).build());
        when(shipmentRepository.findById(5L)).thenReturn(Optional.of(s));
        saveReturnsArgument();
        when(carrierGateway.track(s)).thenReturn(tracking("delivered",
                new CarrierTracking.Event("FS-FS-1-1", "pending_pickup", "Đã nhận", null, null, LocalDateTime.now()),
                new CarrierTracking.Event("FS-FS-1-2", "delivered", "Giao thành công", null, "Hà Nội", LocalDateTime.now())));

        ShipmentResponse res = service.sync(5L, 3L);

        assertEquals("delivered", res.getStatus());
        assertEquals("Tuấn", res.getShipperName());
        assertNotNull(res.getDeliveredAt());
        assertNotNull(res.getLastSyncedAt());
        assertEquals(2, s.getEvents().size(), "event FS-FS-1-1 da co, chi them FS-FS-1-2");
        verify(entityManager).lock(s, LockModeType.PESSIMISTIC_WRITE);
        verify(fulfillment).onShipmentStatus(order, ShipmentStatus.DELIVERED, 3L);
    }

    @Test
    void syncDoesNotChangeFinalShipment() {
        Shipment s = shipmentWithStatus(ShipmentStatus.CANCELLED);
        s.setPartnerRef("FS-1");
        when(shipmentRepository.findById(5L)).thenReturn(Optional.of(s));
        saveReturnsArgument();
        when(carrierGateway.track(s)).thenReturn(tracking("in_transit"));

        assertEquals("cancelled", service.sync(5L, 3L).getStatus());
        verifyNoInteractions(fulfillment);
    }

    @Test
    void syncRejectsUnsentOrMismatchedShipment() {
        Shipment unsent = shipmentWithStatus(ShipmentStatus.REQUESTED);
        when(shipmentRepository.findById(5L)).thenReturn(Optional.of(unsent));
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(DeliveryException.class, () -> service.sync(5L, 3L)).getStatus());

        Shipment other = shipmentWithStatus(ShipmentStatus.PENDING_PICKUP);
        other.setPartnerRef("FS-9");
        other.setTrackingCode("FS-9");
        when(shipmentRepository.findById(6L)).thenReturn(Optional.of(other));
        when(carrierGateway.track(other)).thenReturn(tracking("in_transit"));
        assertEquals(HttpStatus.BAD_GATEWAY,
                assertThrows(DeliveryException.class, () -> service.sync(6L, 3L)).getStatus());
    }

    // ---------- danh sach ----------

    @Test
    @SuppressWarnings("unchecked")
    void listRejectsUnknownStatusAndClampsPageSize() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(DeliveryException.class, () -> service.list("lost", null, null, null, 0, 20)).getStatus());

        when(shipmentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(shipmentWithStatus(ShipmentStatus.REQUESTED))));

        var page = service.list("REQUESTED", "fastship", "ORD", 10L, -1, 1000);
        assertEquals(1, page.getContent().size());
        assertNull(page.getContent().get(0).getEvents(), "danh sach khong tra events");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(shipmentRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());
    }

    // ---------- khach hang ----------

    @Test
    void customerSeesOnlyOwnOrderAndHiddenInternalFields() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        Shipment s = shipmentWithStatus(ShipmentStatus.REQUESTED);
        s.setNote("ghi chú nội bộ");
        s.setCreatedBy(3L);
        s.setPartnerRef("P-1");
        when(shipmentRepository.findByOrderIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(s));

        ShipmentResponse res = service.getForCustomer(10L, 7L).orElseThrow();
        assertNull(res.getNote());
        assertNull(res.getCreatedBy());
        assertNull(res.getPartnerRef());

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(DeliveryException.class, () -> service.getForCustomer(10L, 99L)).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(DeliveryException.class, () -> service.getForCustomer(10L, null)).getStatus());
    }

    @Test
    void customerOrderWithoutShipmentReturnsEmpty() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(shipmentRepository.findByOrderIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        assertTrue(service.getForCustomer(10L, 7L).isEmpty());
    }

    @Test
    void customerListHidesInternalFields() {
        Shipment s = shipmentWithStatus(ShipmentStatus.IN_TRANSIT);
        s.setNote("nội bộ");
        when(shipmentRepository.findByOrderUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(s));

        ShipmentResponse r = service.listForCustomer(7L).get(0);
        assertEquals("in_transit", r.getStatus());
        assertNull(r.getNote());
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(DeliveryException.class, () -> service.listForCustomer(null)).getStatus());
    }
}
