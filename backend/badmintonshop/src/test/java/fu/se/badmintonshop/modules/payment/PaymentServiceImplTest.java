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
import java.util.List;
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
    void testCreatePayment_ReusesExistingPendingPayment() {
        Payment existingPayment = Payment.builder()
                .id(201L)
                .orderId(105L)
                .method("qr")
                .status("pending")
                .amount(new BigDecimal("1500000"))
                .build();

        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(105L, "pending"))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);
        when(payOSService.createPaymentLink(anyLong(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of(
                        "checkoutUrl", "https://pay.payos.vn/web/test",
                        "qrCodeUrl", "https://img.vietqr.io/image/test.png",
                        "paymentLinkId", "link-105"
                ));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(105L)
                .amount(new BigDecimal("1500000"))
                .build();

        CreatePaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(201L, response.getPaymentId());
        assertEquals("pending", response.getStatus());
        verify(paymentRepository, atLeastOnce()).save(existingPayment);
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

    @Test
    void testCancelPayment_Success() {
        when(paymentRepository.findById(201L)).thenReturn(Optional.of(testPayment));
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));

        PaymentDetailResponse response = paymentService.cancelPayment(201L, "Đổi ý không mua nữa");

        assertNotNull(response);
        assertEquals("cancelled", testPayment.getStatus());
        assertEquals("pending", testOrder.getStatus());
        assertEquals("unpaid", testOrder.getPaymentStatus());
        assertNull(testOrder.getCancelReason());
        assertNull(testPayment.getQrCodeUrl());
        assertNull(testPayment.getCheckoutUrl());
        verify(paymentRepository).save(testPayment);
        verify(orderRepository, never()).save(any());
        verify(payOSService).cancelPaymentLink(eq(100201L), eq("Đổi ý không mua nữa"));
    }

    @Test
    void testCancelPayment_AlreadyPaid_ThrowsException() {
        testPayment.setStatus("paid");
        when(paymentRepository.findById(201L)).thenReturn(Optional.of(testPayment));

        assertThrows(IllegalStateException.class, () ->
                paymentService.cancelPayment(201L, "Hủy"));
    }

    @Test
    void testCancelPaymentByOrderId_Success() {
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(105L)).thenReturn(List.of(testPayment));
        when(paymentRepository.findById(201L)).thenReturn(Optional.of(testPayment));
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));

        PaymentDetailResponse response = paymentService.cancelPaymentByOrderId(105L, "Khách hàng hủy");

        assertNotNull(response);
        assertEquals("cancelled", testPayment.getStatus());
        assertEquals("pending", testOrder.getStatus());
        verify(orderRepository, never()).save(any());
        verify(paymentRepository).save(testPayment);
    }
    @Test
    void cancelledSessionCanBeFollowedByNewPaymentWithoutCancellingOrder() {
        when(paymentRepository.findById(201L)).thenReturn(Optional.of(testPayment));
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getId() == null) { p.setId(202L); p.setCreatedAt(LocalDateTime.now()); }
            return p;
        });
        paymentService.cancelPayment(201L, "Tạo mã QR mới");
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(105L, "pending")).thenReturn(Optional.empty());
        when(payOSService.createPaymentLink(anyLong(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of("checkoutUrl", "https://pay.payos.vn/new", "qrCodeUrl", "https://example.com/new-qr.png"));
        CreatePaymentResponse next = paymentService.createPayment(CreatePaymentRequest.builder().orderId(105L).build());
        assertEquals(202L, next.getPaymentId());
        assertEquals("pending", next.getStatus());
        assertEquals("https://pay.payos.vn/new", next.getCheckoutUrl());
        assertEquals("cancelled", testPayment.getStatus());
        assertEquals("pending", testOrder.getStatus());
        assertEquals("unpaid", testOrder.getPaymentStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancellingWithoutPendingSessionDoesNotCancelOrder() {
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(105L)).thenReturn(List.of());
        assertNull(paymentService.cancelPaymentByOrderId(105L, "Hủy phiên"));
        assertEquals("pending", testOrder.getStatus());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(payOSService);
    }

    @Test
    void closingProviderLinkFailureMustPropagateBeforeOrderCancellation() {
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(105L)).thenReturn(List.of(testPayment));
        doThrow(new IllegalStateException("Already paid")).when(payOSService).cancelPaymentLink(anyLong(), anyString());
        assertThrows(IllegalStateException.class, () -> paymentService.closePendingPaymentLinks(105L, "Hủy"));
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void pendingOrderWithoutGeneratedQrDoesNotNeedProviderCancellation() {
        testPayment.setQrCodeUrl(null);
        testPayment.setCheckoutUrl(null);
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(105L)).thenReturn(List.of(testPayment));
        paymentService.closePendingPaymentLinks(105L, "Hủy");
        verifyNoInteractions(payOSService);
    }

    @Test
    void cancellationCannotReopenOrRestockCancelledOrder() {
        testOrder.setStatus("cancelled");
        testPayment.setStatus("cancelled");
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(105L)).thenReturn(List.of(testPayment));
        paymentService.cancelPaymentByOrderId(105L, "Retry");
        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(payOSService);
    }

    @Test
    void unpaidShippingOrderCannotCancelPayment() {
        testOrder.setStatus("shipping");
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));
        assertThrows(IllegalStateException.class, () -> paymentService.cancelPaymentByOrderId(105L, "Hủy"));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(payOSService);
    }

    @Test
    void latePaymentCallbackDoesNotReopenCancelledOrder() {
        testOrder.setStatus("cancelled");
        testPayment.setStatus("cancelled");
        when(orderRepository.findById(105L)).thenReturn(Optional.of(testOrder));
        paymentService.markPaymentAsPaid(testPayment, "late-payment");
        assertEquals("cancelled", testOrder.getStatus());
        assertEquals("paid", testOrder.getPaymentStatus());
    }
}

