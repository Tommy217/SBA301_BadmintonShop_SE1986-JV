package fu.se.badmintonshop.modules.delivery;

import fu.se.badmintonshop.modules.delivery.dto.CancelShipmentRequest;
import fu.se.badmintonshop.modules.delivery.dto.CreateShipmentRequest;
import fu.se.badmintonshop.modules.delivery.dto.ShipmentResponse;
import fu.se.badmintonshop.modules.delivery.entity.Carrier;
import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.gateway.CarrierGateway;
import fu.se.badmintonshop.modules.delivery.repository.CarrierRepository;
import fu.se.badmintonshop.modules.delivery.repository.ShipmentRepository;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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

    @InjectMocks
    ShipmentService service;

    Order order;
    Carrier fastship;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(10L).orderCode("ORD-10").userId(7L).status("confirmed")
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

    @Test
    void createSavesRequestedShipmentWithEventAndCallsGateway() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderIdAndStatusNotIn(eq(10L), anyCollection())).thenReturn(false);
        when(carrierRepository.findByCode("fastship")).thenReturn(Optional.of(fastship));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            s.setId(5L);
            return s;
        });

        ShipmentResponse res = service.create(createRequest(), 3L);

        assertEquals(5L, res.getId());
        assertEquals("requested", res.getStatus());
        assertEquals("fast", res.getServiceType());
        assertEquals("ORD-10", res.getOrderCode());
        assertEquals("fastship", res.getCarrierCode());
        assertEquals(3L, res.getCreatedBy());
        assertTrue(res.getCancellable());
        assertEquals(1, res.getEvents().size());
        assertEquals("manual", res.getEvents().get(0).getSource());
        verify(carrierGateway).requestShipment(any(Shipment.class));
    }

    @Test
    void createDefaultsToStandardService() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(carrierRepository.findByCode("fastship")).thenReturn(Optional.of(fastship));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateShipmentRequest req = createRequest();
        req.setServiceType(null);

        assertEquals("standard", service.create(req, null).getServiceType());
    }

    @Test
    void createRejectsOrderNotReadyToShip() {
        order.setStatus("pending");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        DeliveryException e = assertThrows(DeliveryException.class, () -> service.create(createRequest(), 3L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(carrierGateway);
    }

    @Test
    void createRejectsWhenOrderAlreadyHasActiveShipment() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderIdAndStatusNotIn(eq(10L), argThat(c ->
                c.containsAll(List.of("cancelled", "rejected")) && c.size() == 2))).thenReturn(true);

        DeliveryException e = assertThrows(DeliveryException.class, () -> service.create(createRequest(), 3L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveOrUnknownCarrier() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
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
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(DeliveryException.class, () -> service.create(createRequest(), 3L)).getStatus());
    }

    @Test
    void cancelBeforePickupMarksCancelled() {
        Shipment s = shipmentWithStatus(ShipmentStatus.PENDING_PICKUP);
        when(shipmentRepository.findWithDetailsById(5L)).thenReturn(Optional.of(s));
        when(shipmentRepository.save(s)).thenReturn(s);

        ShipmentResponse res = service.cancel(5L, new CancelShipmentRequest("  Khách đổi ý  "));

        assertEquals("cancelled", res.getStatus());
        assertEquals("Khách đổi ý", res.getCancelReason());
        assertNotNull(res.getCancelledAt());
        assertFalse(res.getCancellable());
        assertEquals("cancelled", s.getEvents().get(0).getStatus());
        verify(carrierGateway).cancelShipment(s, "Khách đổi ý");
    }

    @Test
    void cancelAfterPickupIsRejected() {
        Shipment s = shipmentWithStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findWithDetailsById(5L)).thenReturn(Optional.of(s));

        DeliveryException e = assertThrows(DeliveryException.class,
                () -> service.cancel(5L, new CancelShipmentRequest("x")));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("in_transit", s.getStatus());
        verifyNoInteractions(carrierGateway);
    }

    @Test
    void listRejectsUnknownStatusAndClampsPageSize() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(DeliveryException.class, () -> service.list("lost", null, null, 0, 20)).getStatus());

        when(shipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(shipmentWithStatus(ShipmentStatus.REQUESTED))));

        var page = service.list("REQUESTED", "fastship", "ORD", -1, 1000);
        assertEquals(1, page.getContent().size());
        assertNull(page.getContent().get(0).getEvents(), "danh sach khong tra events");

        var captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(shipmentRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());
    }

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
}
