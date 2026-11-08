package fu.se.badmintonshop.modules.supportticket;

import fu.se.badmintonshop.modules.common.repository.SystemSettingRepository;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.entity.OrderItem;
import fu.se.badmintonshop.modules.order.repository.OrderItemRepository;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.payment.repository.PaymentRepository;
import fu.se.badmintonshop.modules.supportticket.dto.*;
import fu.se.badmintonshop.modules.supportticket.entity.*;
import fu.se.badmintonshop.modules.supportticket.enums.InspectionResult;
import fu.se.badmintonshop.modules.supportticket.enums.TicketCause;
import fu.se.badmintonshop.modules.supportticket.enums.TicketStatus;
import fu.se.badmintonshop.modules.supportticket.repository.*;
import fu.se.badmintonshop.modules.supportticket.repository.SupportTicketRepository;
import fu.se.badmintonshop.modules.supportticket.service.impl.SupportTicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository ticketRepository;
    @Mock
    private SupportTicketItemRepository itemRepository;
    @Mock
    private TicketEvidenceRepository evidenceRepository;
    @Mock
    private TicketStatusHistoryRepository statusHistoryRepository;
    @Mock
    private TicketInspectionRepository inspectionRepository;
    @Mock
    private RefundRecordRepository refundRecordRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private SupportTicketServiceImpl ticketService;

    private Order sampleOrder;
    private OrderItem sampleOrderItem;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .id(1L)
                .orderCode("BS-ORD-1001")
                .userId(10L)
                .totalAmount(new BigDecimal("1500000"))
                .shippingFee(new BigDecimal("30000"))
                .status("completed")
                .paymentStatus("paid")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        sampleOrderItem = OrderItem.builder()
                .id(101L)
                .orderId(1L)
                .productId(201L)
                .variantId(301L)
                .productName("Vá»£t Yonex Astrox 88D Pro")
                .unitPrice(new BigDecimal("1470000"))
                .quantity(1)
                .lineTotal(new BigDecimal("1470000"))
                .build();
    }

    @Test
    @DisplayName("Khách hàng tạo phiếu REFUND hợp lệ với QR và sản phẩm")
    void testCreateTicket_Refund_Success() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .orderId(1L)
                .ticketType("REFUND")
                .reason("Vợt bị nứt khung")
                .description("Vừa nhận hàng mở hộp đã thấy nứt")
                .refundQrUrl("https://example.com/qr.png")
                .evidenceUrls(List.of("https://example.com/evidence1.jpg"))
                .items(List.of(new TicketItemRequest(101L, 1)))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(ticketRepository.existsByOrderIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);
        when(ticketRepository.existsByOrderIdAndStatus(1L, TicketStatus.REFUNDED.getValue())).thenReturn(false);
        when(ticketRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(ticketRepository.countByOrderIdAndStatusNot(1L, TicketStatus.CANCELLED.getValue())).thenReturn(0L);
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(sampleOrderItem));

        SupportTicket savedTicket = SupportTicket.builder()
                .id(501L)
                .ticketCode("ST-20261031-12345")
                .userId(10L)
                .orderId(1L)
                .ticketType("REFUND")
                .status("PENDING")
                .reason(request.getReason())
                .refundQrUrl(request.getRefundQrUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(ticketRepository.save(any(SupportTicket.class))).thenReturn(savedTicket);
        when(itemRepository.save(any(SupportTicketItem.class))).thenReturn(SupportTicketItem.builder().id(1L).ticketId(501L).orderItemId(101L).quantity(1).build());
        when(evidenceRepository.save(any(TicketEvidence.class))).thenReturn(TicketEvidence.builder().id(1L).ticketId(501L).fileUrl("url").build());

        TicketResponse response = ticketService.createTicket(request, 10L);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals("REFUND", response.getTicketType());
        verify(statusHistoryRepository, times(1)).save(any(TicketStatusHistory.class));
    }

    @Test
    @DisplayName("Tạo phiếu thất bại nếu đơn hàng đang có ticket trong tiến trình xử lý")
    void testCreateTicket_DuplicateActiveOrder_ThrowsException() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .orderId(1L)
                .ticketType("REFUND")
                .reason("Lỗi")
                .refundQrUrl("url")
                .evidenceUrls(List.of("url"))
                .items(List.of(new TicketItemRequest(101L, 1)))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(ticketRepository.existsByOrderIdAndStatusIn(eq(1L), anyCollection())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ticketService.createTicket(request, 10L));
        assertTrue(ex.getMessage().contains("đang được xử lý"));
    }

    @Test
    @DisplayName("Khách hàng được phép tạo lại ticket mới sau khi phiếu cũ bị REJECTED ở bước sơ duyệt")
    void testCreateTicket_AfterInitialReject_AllowsRetry() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .orderId(1L)
                .ticketType("REFUND")
                .reason("Chụp lại ảnh bằng chứng rõ nét hơn")
                .description("Gửi lại bằng chứng theo yêu cầu của nhân viên")
                .refundQrUrl("https://example.com/qr.png")
                .evidenceUrls(List.of("https://example.com/evidence_clear.jpg"))
                .items(List.of(new TicketItemRequest(101L, 1)))
                .build();

        SupportTicket oldRejectedTicket = SupportTicket.builder()
                .id(401L)
                .orderId(1L)
                .status(TicketStatus.REJECTED.getValue())
                .rejectReason("Ảnh chụp mờ, vui lòng gửi lại")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(ticketRepository.existsByOrderIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);
        when(ticketRepository.existsByOrderIdAndStatus(1L, TicketStatus.REFUNDED.getValue())).thenReturn(false);
        when(ticketRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(oldRejectedTicket));
        when(inspectionRepository.findByTicketId(401L)).thenReturn(Optional.empty()); // Chưa kiểm định tại kho
        when(ticketRepository.countByOrderIdAndStatusNot(1L, TicketStatus.CANCELLED.getValue())).thenReturn(1L); // 1 lần reject trước đó < MAX (2)
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(sampleOrderItem));

        SupportTicket newTicket = SupportTicket.builder()
                .id(502L)
                .ticketCode("ST-20261108-99999")
                .userId(10L)
                .orderId(1L)
                .ticketType("REFUND")
                .status("PENDING")
                .reason(request.getReason())
                .refundQrUrl(request.getRefundQrUrl())
                .build();

        when(ticketRepository.save(any(SupportTicket.class))).thenReturn(newTicket);
        when(itemRepository.save(any(SupportTicketItem.class))).thenReturn(SupportTicketItem.builder().id(2L).ticketId(502L).orderItemId(101L).quantity(1).build());
        when(evidenceRepository.save(any(TicketEvidence.class))).thenReturn(TicketEvidence.builder().id(2L).ticketId(502L).fileUrl("url").build());

        TicketResponse response = ticketService.createTicket(request, 10L);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
    }

    @Test
    @DisplayName("Chặn tạo ticket nếu đơn hàng đã vượt quá số lần tối đa (>= 2 lần)")
    void testCreateTicket_ExceedMaxAttempts_ThrowsException() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .orderId(1L)
                .ticketType("REFUND")
                .reason("Khiếu nại lần 3")
                .refundQrUrl("url")
                .evidenceUrls(List.of("url"))
                .items(List.of(new TicketItemRequest(101L, 1)))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(ticketRepository.existsByOrderIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);
        when(ticketRepository.existsByOrderIdAndStatus(1L, TicketStatus.REFUNDED.getValue())).thenReturn(false);
        when(ticketRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(ticketRepository.countByOrderIdAndStatusNot(1L, TicketStatus.CANCELLED.getValue())).thenReturn(2L); // Đã có 2 phiếu

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ticketService.createTicket(request, 10L));
        assertTrue(ex.getMessage().contains("đã đạt giới hạn tối đa"));
    }

    @Test
    @DisplayName("Chặn tạo ticket nếu sản phẩm đã bị từ chối sau kiểm định thực tế tại kho")
    void testCreateTicket_AfterInspectionFailed_ThrowsException() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .orderId(1L)
                .ticketType("REFUND")
                .reason("Cố gắng khiếu nại lại sau khi kho trả hàng")
                .refundQrUrl("url")
                .evidenceUrls(List.of("url"))
                .items(List.of(new TicketItemRequest(101L, 1)))
                .build();

        SupportTicket failedTicket = SupportTicket.builder()
                .id(402L)
                .orderId(1L)
                .status(TicketStatus.INSPECTION_FAILED.getValue())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(ticketRepository.existsByOrderIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);
        when(ticketRepository.existsByOrderIdAndStatus(1L, TicketStatus.REFUNDED.getValue())).thenReturn(false);
        when(ticketRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(failedTicket));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ticketService.createTicket(request, 10L));
        assertTrue(ex.getMessage().contains("kiểm định sản phẩm thực tế"));
    }

    @Test
    @DisplayName("NhÃ¢n viÃªn sÆ¡ duyá»‡t tiáº¿p nháº­n phiáº¿u vÃ  chá»n nguyÃªn nhÃ¢n STORE_FAULT")
    void testReviewTicket_Approve_Success() {
        SupportTicket ticket = SupportTicket.builder()
                .id(501L)
                .ticketCode("ST-100")
                .orderId(1L)
                .ticketType("REFUND")
                .status("PENDING")
                .build();

        when(ticketRepository.findById(501L)).thenReturn(Optional.of(ticket));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(ticketRepository.save(any())).thenReturn(ticket);

        ReviewTicketRequest request = ReviewTicketRequest.builder()
                .action("approve")
                .cause(TicketCause.STORE_FAULT.getValue())
                .staffId(2L)
                .build();

        TicketResponse response = ticketService.reviewTicket(501L, request);

        assertEquals(TicketStatus.AWAITING_RETURN.getValue(), response.getStatus());
        assertEquals(TicketCause.STORE_FAULT.getValue(), response.getCause());
    }

    @Test
    @DisplayName("NhÃ¢n viÃªn tháº©m Ä‘á»‹nh Äáº T vá»›i sá»‘ tiá»n hoÃ n < 3 triá»‡u -> Chuyá»ƒn PENDING_TRANSFER")
    void testInspectTicket_Passed_Under3M_MovesToPendingTransfer() {
        SupportTicket ticket = SupportTicket.builder()
                .id(501L)
                .ticketCode("ST-100")
                .orderId(1L)
                .ticketType("REFUND")
                .status("AWAITING_RETURN")
                .cause(TicketCause.STORE_FAULT.getValue())
                .build();

        when(ticketRepository.findById(501L)).thenReturn(Optional.of(ticket));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(sampleOrderItem));
        when(itemRepository.findByTicketId(501L)).thenReturn(List.of(
                SupportTicketItem.builder().ticketId(501L).orderItemId(101L).quantity(1).build()
        ));
        when(ticketRepository.save(any())).thenReturn(ticket);

        InspectionRequest request = InspectionRequest.builder()
                .result(InspectionResult.PASSED.getValue())
                .note("HÃ ng cÃ²n nguyÃªn tem báº£o hÃ nh")
                .staffId(2L)
                .build();

        TicketResponse response = ticketService.inspectTicket(501L, request);

        assertEquals(TicketStatus.PENDING_TRANSFER.getValue(), response.getStatus());
        // 1,470,000 + 30,000 ship fee (vÃ¬ STORE_FAULT) = 1,500,000
        assertEquals(new BigDecimal("1500000"), response.getRefundAmount());
    }

    @Test
    @DisplayName("Thẩm định ĐẠT nhưng lý do là CUSTOMER_PREFERENCE -> Từ chối phiếu (REJECTED)")
    void testInspectTicket_Passed_CustomerPreference_RejectsTicket() {
        SupportTicket ticket = SupportTicket.builder()
                .id(502L)
                .ticketCode("ST-102")
                .orderId(1L)
                .ticketType("REFUND")
                .status("AWAITING_RETURN")
                .cause(TicketCause.CUSTOMER_PREFERENCE.getValue())
                .build();

        when(ticketRepository.findById(502L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        InspectionRequest request = InspectionRequest.builder()
                .result(InspectionResult.PASSED.getValue())
                .note("Hàng nguyên vẹn không lỗi")
                .staffId(2L)
                .build();

        TicketResponse response = ticketService.inspectTicket(502L, request);

        assertEquals(TicketStatus.REJECTED.getValue(), response.getStatus());
        assertNotNull(response.getRejectReason());
    }

    @Test
    @DisplayName("NhÃ¢n viÃªn chuyá»ƒn khoáº£n hoÃ n tiá»n thÃ nh cÃ´ng vÃ  lÆ°u mÃ£ giao dá»‹ch")
    void testRecordRefundTransfer_Success() {
        SupportTicket ticket = SupportTicket.builder()
                .id(501L)
                .ticketCode("ST-100")
                .orderId(1L)
                .ticketType("REFUND")
                .status("PENDING_TRANSFER")
                .refundAmount(new BigDecimal("1500000"))
                .build();

        when(ticketRepository.findById(501L)).thenReturn(Optional.of(ticket));
        when(refundRecordRepository.existsByBankReference("FT2610319999")).thenReturn(false);
        when(refundRecordRepository.findByTicketId(501L)).thenReturn(Optional.empty());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        RefundTransferRequest request = RefundTransferRequest.builder()
                .bankReference("FT2610319999")
                .note("ÄÃ£ chuyá»ƒn khoáº£n Napas 24/7")
                .staffId(2L)
                .build();

        TicketResponse response = ticketService.recordRefundTransfer(501L, request);

        assertEquals(TicketStatus.REFUNDED.getValue(), response.getStatus());
        verify(refundRecordRepository, times(1)).save(any(RefundRecord.class));
    }

    @Test
    @DisplayName("Hệ thống chỉ chấp nhận yêu cầu REFUND -> Ném ngoại lệ khi gửi loại yêu cầu khác")
    void testCreateTicket_NonRefundType_ThrowsException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(ticketRepository.existsByOrderIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);
        when(ticketRepository.existsByOrderIdAndStatus(1L, TicketStatus.REFUNDED.getValue())).thenReturn(false);
        when(ticketRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(ticketRepository.countByOrderIdAndStatusNot(1L, TicketStatus.CANCELLED.getValue())).thenReturn(0L);

        CreateTicketRequest request = CreateTicketRequest.builder()
                .orderId(1L)
                .ticketType("OTHER")
                .reason("Không muốn nhận nữa")
                .refundQrUrl("https://example.com/qr.png")
                .evidenceUrls(List.of("https://example.com/evidence1.jpg"))
                .items(List.of(new TicketItemRequest(101L, 1)))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ticketService.createTicket(request, 10L));
        assertTrue(ex.getMessage().contains("REFUND"));
    }
}

