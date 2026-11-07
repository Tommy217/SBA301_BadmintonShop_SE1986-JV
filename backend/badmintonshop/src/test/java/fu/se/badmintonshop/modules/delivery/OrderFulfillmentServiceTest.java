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
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceTest {

    @Mock
    JdbcTemplate jdbc;
    @Mock
    InvoiceService invoiceService;
    @Mock
    InvoiceRepository invoiceRepository;

    @InjectMocks
    OrderFulfillmentService fulfillment;

    private Order order(String status, String paymentStatus) {
        return Order.builder().id(10L).orderCode("ORD-10").status(status).paymentStatus(paymentStatus)
                .subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).build();
    }

    @Test
    void pickupMovesConfirmedOrderToShipping() {
        Order o = order("confirmed", "paid");
        assertEquals("shipping", fulfillment.onShipmentStatus(o, ShipmentStatus.PENDING_PICKUP, 3L));
        assertEquals("shipping", o.getStatus());
        verify(jdbc).update(startsWith("INSERT INTO order_status_history"), eq(10L), eq("confirmed"), eq("shipping"), eq(3L), anyString(), any());
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
        when(jdbc.queryForList(startsWith("SELECT variant_id,quantity"), eq(10L)))
                .thenReturn(List.of(Map.of("variant_id", 4L, "quantity", 2)));

        assertEquals("returned", fulfillment.onShipmentStatus(o, ShipmentStatus.RETURNED, 3L));
        verify(jdbc).update(startsWith("UPDATE product_variants SET stock_quantity=stock_quantity+?"), eq(2), eq(4L));
        verify(jdbc).update(startsWith("INSERT INTO inventory_movements"), eq(4L), eq(2), eq(4L), eq(10L), anyString(), any());
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
        verifyNoInteractions(jdbc, invoiceService);
    }
}
