package fu.se.badmintonshop.modules.payment.service;

import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.payment.entity.Payment;
import fu.se.badmintonshop.modules.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAccessTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentAccess paymentAccess;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityUser(Long userId, String role) {
        CustomUserDetails userDetails = new CustomUserDetails(
                userId, "user@test.com", "Test User", "pass", true, false, role
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testOrder_Unauthenticated_ThrowsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> paymentAccess.order(1L));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Vui lòng đăng nhập.", ex.getReason());
    }

    @Test
    void testOrder_OrderNotFound_ThrowsNotFound() {
        setSecurityUser(100L, "CUSTOMER");
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> paymentAccess.order(1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Không tìm thấy đơn hàng.", ex.getReason());
    }

    @Test
    void testOrder_DifferentUser_ThrowsForbidden() {
        setSecurityUser(100L, "CUSTOMER");
        Order order = Order.builder().id(1L).userId(200L).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> paymentAccess.order(1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Bạn không có quyền truy cập đơn hàng này.", ex.getReason());
    }

    @Test
    void testOrder_SameUser_Success() {
        setSecurityUser(100L, "CUSTOMER");
        Order order = Order.builder().id(1L).userId(100L).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertDoesNotThrow(() -> paymentAccess.order(1L));
    }

    @Test
    void testOrder_ManagementUser_AccessOtherUserOrder_Success() {
        setSecurityUser(999L, "STAFF");
        Order order = Order.builder().id(1L).userId(100L).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertDoesNotThrow(() -> paymentAccess.order(1L));
    }

    @Test
    void testPayment_NotFound_ThrowsNotFound() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> paymentAccess.payment(10L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Không tìm thấy thanh toán.", ex.getReason());
    }

    @Test
    void testPayment_Found_ValidatesOrderSuccessfully() {
        setSecurityUser(100L, "CUSTOMER");
        Payment payment = Payment.builder().id(10L).orderId(1L).build();
        Order order = Order.builder().id(1L).userId(100L).build();

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertDoesNotThrow(() -> paymentAccess.payment(10L));
    }
}

