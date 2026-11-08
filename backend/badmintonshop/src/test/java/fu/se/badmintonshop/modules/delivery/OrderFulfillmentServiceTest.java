package fu.se.badmintonshop.modules.delivery;

import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.delivery.service.OrderFulfillmentService;
import fu.se.badmintonshop.modules.invoice.repository.InvoiceRepository;
import fu.se.badmintonshop.modules.invoice.service.InvoiceService;
import fu.se.badmintonshop.modules.order.entity.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.persistence.*;
import fu.se.badmintonshop.modules.order.entity.OrderItem;
import fu.se.badmintonshop.modules.order.entity.OrderStatusHistory;
import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;
import fu.se.badmintonshop.modules.inventory.entity.InventoryMovement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceTest {

    @Mock EntityManager em;
    @Mock TypedQuery<OrderItem> lines;
    @Mock InvoiceService invoiceService;
    @Mock InvoiceRepository invoiceRepository;

    @InjectMocks OrderFulfillmentService fulfillment;

    private Order order(String status, String paymentStatus) {
        return Order.builder()
                .id(10L)
                .orderCode("ORD-10")
                .status(status)
                .paymentStatus(paymentStatus)
                .subtotal(BigDecimal.TEN)
                .totalAmount(BigDecimal.TEN)
                .build();
    }

    @Test
    void pickupMovesConfirmedOrderToShipping() {
        Order o = order("confirmed", "paid");
        assertEquals(
                "shipping", fulfillment.onShipmentStatus(o, ShipmentStatus.PENDING_PICKUP, 3L));
        assertEquals("shipping", o.getStatus());
        verify(em)
                .persist(
                        argThat(
                                value ->
                                        value instanceof OrderStatusHistory h
                                                && h.getOrderId() == 10L
                                                && h.getFromStatus().equals("confirmed")
                                                && h.getToStatus().equals("shipping")
                                                && h.getChangedBy() == 3L));
    }

    @Test
    void deliveredCompletesOrderAndIssuesInvoiceOnce() {
        Order o = order("shipping", "paid");
        when(invoiceRepository.existsByOrderIdAndInvoiceType(10L, "sale")).thenReturn(false);

        assertEquals("completed", fulfillment.onShipmentStatus(o, ShipmentStatus.DELIVERED, 3L));
        verify(invoiceService).create(argThat(r -> r.getOrderId() == 10L));

        Order again = order("shipping", "paid");
        when(invoiceRepository.existsByOrderIdAndInvoiceType(10L, "sale")).thenReturn(true);
        fulfillment.onShipmentStatus(again, ShipmentStatus.DELIVERED, 3L);
        verifyNoMoreInteractions(invoiceService);
    }

    @Test
    void deliveredUnpaidOrderGetsNoInvoice() {
        Order o = order("shipping", "unpaid");
        assertEquals("completed", fulfillment.onShipmentStatus(o, ShipmentStatus.DELIVERED, 3L));
        verifyNoInteractions(invoiceService);
    }

    @Test
    void returnedRestocksItems() {
        Order o = order("shipping", "paid");
        when(em.createQuery(anyString(), eq(OrderItem.class))).thenReturn(lines);
        when(lines.setParameter("id", 10L)).thenReturn(lines);
        when(lines.getResultList())
                .thenReturn(List.of(OrderItem.builder().variantId(4L).quantity(2).build()));
        var variant = ProductVariant.builder().id(4L).stockQuantity(5).build();
        when(em.find(ProductVariant.class, 4L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(variant);

        assertEquals("returned", fulfillment.onShipmentStatus(o, ShipmentStatus.RETURNED, 3L));
        assertEquals(7, variant.getStockQuantity());
        verify(em)
                .persist(
                        argThat(
                                value ->
                                        value instanceof InventoryMovement m
                                                && m.getQuantity() == 2
                                                && m.getBalanceAfter() == 7
                                                && m.getRefId() == 10L));
    }

    @Test
    void cancelledShipmentReturnsShippingOrderToPreparing() {
        Order o = order("shipping", "paid");
        assertEquals("preparing", fulfillment.onShipmentStatus(o, ShipmentStatus.CANCELLED, 3L));

        Order confirmed = order("confirmed", "paid");
        assertNull(fulfillment.onShipmentStatus(confirmed, ShipmentStatus.CANCELLED, 3L));
        assertEquals("confirmed", confirmed.getStatus());
    }

    @Test
    void finishedOrdersAreNotTouched() {
        for (String status : List.of("completed", "cancelled", "returned")) {
            Order o = order(status, "paid");
            assertNull(fulfillment.onShipmentStatus(o, ShipmentStatus.DELIVERED, 3L), status);
            assertEquals(status, o.getStatus());
        }
        Order shipping = order("shipping", "paid");
        assertNull(fulfillment.onShipmentStatus(shipping, ShipmentStatus.IN_TRANSIT, 3L));
        verifyNoInteractions(em, invoiceService);
    }
}
