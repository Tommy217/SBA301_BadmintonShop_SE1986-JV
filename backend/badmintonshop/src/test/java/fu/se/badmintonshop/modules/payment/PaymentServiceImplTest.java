package fu.se.badmintonshop.modules.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.payment.dto.request.CreatePaymentRequest;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookData;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.response.CreatePaymentResponse;
import fu.se.badmintonshop.modules.payment.dto.response.PaymentDetailResponse;
import fu.se.badmintonshop.modules.payment.entity.Payment;
import fu.se.badmintonshop.modules.payment.repository.PaymentRepository;
import fu.se.badmintonshop.modules.payment.repository.PaymentTransactionRepository;
import fu.se.badmintonshop.modules.payment.service.PayOSService;
import fu.se.badmintonshop.modules.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PayOSService payOSService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(105L)
                .orderCode("ORD-105")
                .status("pending")
                .paymentStatus("unpaid")
                .totalAmount(new BigDecimal("1500000"))
                .receiverName("Nguyen Van A")
                .receiverPhone("0912345678")
                .build();

        testPayment = Payment.builder()
                .id(201L)
                .orderId(105L)
                .method("qr")
                .gateway("payos")
                .status("pending")
                .amount(new BigDecimal("1500000"))
                .gatewayTxnRef("100201")
                .qrCodeUrl("https://img.vietqr.io/image/...")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreatePayment_Success() {
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(payOSService.createPaymentLink(anyLong(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of(
                        "checkoutUrl", "https://pay.payos.vn/web/test",
                        "qrCodeUrl", "https://img.vietqr.io/image/test.png",
                        "paymentLinkId", "link-105"
                ));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(105L)
                .amount(new BigDecimal("1500000"))
                .description("Don hang 105")
                .buyerName("Nguyen Van A")
                .build();

        CreatePaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(201L, response.getPaymentId());
        assertEquals("pending", response.getStatus());
        assertEquals(new BigDecimal("1500000"), response.getAmount());
        assertNotNull(response.getPaymentCode());
        assertEquals("https://pay.payos.vn/web/test", response.getCheckoutUrl());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
    }

    @Test
    void testCreatePayment_OrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(999L)
                .build();

        assertThrows(IllegalArgumentException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void testCreatePayment_AlreadyPaid() {
        testOrder.setPaymentStatus("paid");
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(105L)
                .build();

        assertThrows(IllegalStateException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void testHandlePayOSWebhook_Success() {
        when(payOSService.verifyWebhookSignature(any())).thenReturn(true);
        when(paymentRepository.findById(201L)).thenReturn(Optional.of(testPayment));
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));

        PayOSWebhookData data = PayOSWebhookData.builder()
                .orderCode(100201L) // 100000 + 201
                .amount(new BigDecimal("1500000"))
                .reference("FT2611018899")
                .code("00")
                .build();

        PayOSWebhookRequest webhookRequest = PayOSWebhookRequest.builder()
                .code("00")
                .desc("success")
                .data(data)
                .signature("valid_signature")
                .build();

        paymentService.handlePayOSWebhook(webhookRequest);

        assertEquals("paid", testPayment.getStatus());
        assertNotNull(testPayment.getPaidAt());
        assertEquals("FT2611018899", testPayment.getGatewayTxnRef());

        assertEquals("paid", testOrder.getPaymentStatus());
        assertEquals("confirmed", testOrder.getStatus());

        verify(paymentRepository).save(testPayment);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testHandlePayOSWebhook_Idempotency() {
        testPayment.setStatus("paid");
        when(payOSService.verifyWebhookSignature(any())).thenReturn(true);
        when(paymentRepository.findById(201L)).thenReturn(Optional.of(testPayment));

        PayOSWebhookData data = PayOSWebhookData.builder()
                .orderCode(100201L)
                .amount(new BigDecimal("1500000"))
                .reference("FT2611018899")
                .build();

        PayOSWebhookRequest webhookRequest = PayOSWebhookRequest.builder()
                .code("00")
                .data(data)
                .signature("valid_signature")
                .build();

        // Gửi webhook lần 2 khi payment đã paid -> không cập nhật lại
        paymentService.handlePayOSWebhook(webhookRequest);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void testHandlePayOSWebhook_InvalidSignature() {
        when(payOSService.verifyWebhookSignature(any())).thenReturn(false);

        PayOSWebhookData data = PayOSWebhookData.builder()
                .orderCode(100201L)
                .build();

        PayOSWebhookRequest webhookRequest = PayOSWebhookRequest.builder()
                .data(data)
                .signature("tampered_signature")
                .build();

        assertThrows(SecurityException.class, () -> paymentService.handlePayOSWebhook(webhookRequest));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testGetPaymentById_Success() {
        when(paymentRepository.findById(201L)).thenReturn(Optional.of(testPayment));
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));

        PaymentDetailResponse response = paymentService.getPaymentById(201L);

        assertNotNull(response);
        assertEquals(201L, response.getId());
        assertEquals(105L, response.getOrderId());
        assertEquals("ORD-105", response.getOrderCode());
        assertEquals("pending", response.getStatus());
    }
}

