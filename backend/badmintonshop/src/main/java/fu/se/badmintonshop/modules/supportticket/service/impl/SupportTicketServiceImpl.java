package fu.se.badmintonshop.modules.supportticket.service.impl;

import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;
import fu.se.badmintonshop.modules.catalog.repository.ProductVariantRepository;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.entity.OrderItem;
import fu.se.badmintonshop.modules.order.repository.OrderItemRepository;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.supportticket.dto.*;
import fu.se.badmintonshop.modules.supportticket.entity.*;
import fu.se.badmintonshop.modules.supportticket.enums.*;
import fu.se.badmintonshop.modules.supportticket.repository.*;
import fu.se.badmintonshop.modules.supportticket.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("3000000"); // 3,000,000 VND
    private static final int RETURN_WINDOW_DAYS = 7;

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketItemRepository itemRepository;
    private final TicketEvidenceRepository evidenceRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final TicketInspectionRepository inspectionRepository;
    private final RefundRecordRepository refundRecordRepository;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, Long currentUserId) {
        log.info("Khá»Ÿi táº¡o Support Ticket cho orderId={}, userId={}", request.getOrderId(), currentUserId);

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n hÃ ng ID: " + request.getOrderId()));

        // 1. Kiá»ƒm tra quyá»n sá»Ÿ há»¯u Ä‘Æ¡n hÃ ng
        Long userId = currentUserId != null ? currentUserId : (request.getUserId() != null ? request.getUserId() : order.getUserId());
        if (order.getUserId() != null && userId != null && !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Báº¡n khÃ´ng sá»Ÿ há»¯u Ä‘Æ¡n hÃ ng nÃ y.");
        }

        // 2. Kiá»ƒm tra Ä‘iá»u kiá»‡n 7 ngÃ y ká»ƒ tá»« ngÃ y giao hÃ ng
        if (order.getCreatedAt() != null && order.getCreatedAt().isBefore(LocalDateTime.now().minusDays(RETURN_WINDOW_DAYS + 30))) {
            // LÆ°u Ã½: Trong mÃ´i trÆ°á»ng thá»±c táº¿ kiá»ƒm tra theo delivered_at, táº¡m thá»i cháº¥p nháº­n Ä‘Æ¡n hÃ ng
        }

        // 3. Má»—i Ä‘Æ¡n hÃ ng chá»‰ Ä‘Æ°á»£c táº¡o tá»‘i Ä‘a 1 ticket (CR-BR)
        boolean hasActiveTicket = ticketRepository.existsByOrderIdAndStatusNot(order.getId(), TicketStatus.CANCELLED.getValue());
        if (hasActiveTicket) {
            throw new IllegalStateException("ÄÆ¡n hÃ ng nÃ y Ä‘Ã£ tá»“n táº¡i má»™t phiáº¿u khiáº¿u náº¡i / Ä‘á»•i tráº£ Ä‘ang Ä‘Æ°á»£c xá»­ lÃ½.");
        }

        // 4. Validate loáº¡i ticket
        String type = request.getTicketType().toUpperCase();
        if (!TicketType.REFUND.getValue().equals(type) && !TicketType.EXCHANGE.getValue().equals(type)) {
            throw new IllegalArgumentException("Loáº¡i yÃªu cáº§u khÃ´ng há»£p lá»‡. Pháº£i lÃ  REFUND hoáº·c EXCHANGE.");
        }

        // 5. Báº¯t buá»™c cÃ³ áº£nh QR nháº­n tiá»n
        if (request.getRefundQrUrl() == null || request.getRefundQrUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("áº¢nh mÃ£ QR tÃ i khoáº£n ngÃ¢n hÃ ng nháº­n tiá»n lÃ  báº¯t buá»™c cho cáº£ HoÃ n tiá»n vÃ  Äá»•i hÃ ng.");
        }

        // 6. Báº¯t buá»™c cÃ³ tá»‘i thiá»ƒu 1 file báº±ng chá»©ng
        if (request.getEvidenceUrls() == null || request.getEvidenceUrls().isEmpty()) {
            throw new IllegalArgumentException("Vui lÃ²ng táº£i lÃªn Ã­t nháº¥t má»™t hÃ¬nh áº£nh hoáº·c video báº±ng chá»©ng.");
        }

        // 7. Validate cÃ¡c sáº£n pháº©m yÃªu cáº§u
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        Map<Long, OrderItem> orderItemMap = orderItems.stream().collect(Collectors.toMap(OrderItem::getId, item -> item));

        for (TicketItemRequest it : request.getItems()) {
            OrderItem oi = orderItemMap.get(it.getOrderItemId());
            if (oi == null) {
                throw new IllegalArgumentException("Sáº£n pháº©m ID " + it.getOrderItemId() + " khÃ´ng thuá»™c Ä‘Æ¡n hÃ ng nÃ y.");
            }
            if (it.getQuantity() > oi.getQuantity()) {
                throw new IllegalArgumentException("Sá»‘ lÆ°á»£ng yÃªu cáº§u (" + it.getQuantity() + ") vÆ°á»£t quÃ¡ sá»‘ lÆ°á»£ng Ä‘Ã£ mua (" + oi.getQuantity() + ").");
            }

            // Vá»›i EXCHANGE: Pháº£i cÃ³ replacementVariantId cÃ¹ng sáº£n pháº©m vÃ  cÃ¹ng Ä‘Æ¡n giÃ¡ (CR-BR-05)
            if (TicketType.EXCHANGE.getValue().equals(type)) {
                // Chỉ đổi 1:1 cho chính sản phẩm/biến thể đã mua (không đổi sang sản phẩm khác)
                it.setReplacementVariantId(oi.getVariantId());
            }
        }

        // Táº¡o mÃ£ ticket: ST-yyyyMMdd-XXXXX
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ticketCode = "ST-" + datePrefix + "-" + ThreadLocalRandom.current().nextInt(10000, 99999);

        SupportTicket ticket = SupportTicket.builder()
                .ticketCode(ticketCode)
                .userId(userId != null ? userId : 0L)
                .orderId(order.getId())
                .ticketType(type)
                .status(TicketStatus.PENDING.getValue())
                .reason(request.getReason())
                .description(request.getDescription())
                .refundQrUrl(request.getRefundQrUrl())
                .build();

        SupportTicket savedTicket = ticketRepository.save(ticket);

        // LÆ°u items
        List<SupportTicketItem> savedItems = new ArrayList<>();
        for (TicketItemRequest it : request.getItems()) {
            SupportTicketItem item = SupportTicketItem.builder()
                    .ticketId(savedTicket.getId())
                    .orderItemId(it.getOrderItemId())
                    .quantity(it.getQuantity())
                    .replacementVariantId(it.getReplacementVariantId())
                    .build();
            savedItems.add(itemRepository.save(item));
        }

        // LÆ°u evidence
        List<TicketEvidence> savedEvidences = new ArrayList<>();
        for (String url : request.getEvidenceUrls()) {
            TicketEvidence evidence = TicketEvidence.builder()
                    .ticketId(savedTicket.getId())
                    .fileUrl(url)
                    .fileType(url.contains(".mp4") || url.contains("video") ? "video" : "image")
                    .build();
            savedEvidences.add(evidenceRepository.save(evidence));
        }

        // Ghi nháº­n lá»‹ch sá»­ tráº¡ng thÃ¡i Ä‘áº§u tiÃªn
        addStatusHistory(savedTicket.getId(), null, TicketStatus.PENDING.getValue(), userId, "KhÃ¡ch hÃ ng táº¡o phiáº¿u yÃªu cáº§u há»— trá»£");

        return buildResponse(savedTicket, savedItems, savedEvidences, null, null, null, order);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id, Long currentUserId, boolean isStaffOrManager) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u há»— trá»£ ID: " + id));

        if (!isStaffOrManager && currentUserId != null && !ticket.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("Báº¡n khÃ´ng cÃ³ quyá»n xem phiáº¿u há»— trá»£ nÃ y.");
        }

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(Long currentUserId) {
        if (currentUserId == null) return Collections.emptyList();
        List<SupportTicket> list = ticketRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        return list.stream().map(this::loadFullResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTicketsForStaff(String status, String type) {
        List<SupportTicket> list = ticketRepository.findAllByOrderByCreatedAtDesc();
        return list.stream()
                .filter(t -> status == null || status.equalsIgnoreCase("all") || t.getStatus().equalsIgnoreCase(status))
                .filter(t -> type == null || type.equalsIgnoreCase("all") || t.getTicketType().equalsIgnoreCase(type))
                .map(this::loadFullResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TicketResponse cancelTicket(Long id, Long currentUserId) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u há»— trá»£ ID: " + id));

        if (currentUserId != null && !ticket.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("Báº¡n khÃ´ng cÃ³ quyá»n há»§y phiáº¿u nÃ y.");
        }

        if (!TicketStatus.PENDING.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Chá»‰ cÃ³ thá»ƒ há»§y yÃªu cáº§u khi phiáº¿u á»Ÿ tráº¡ng thÃ¡i Chá» tiáº¿p nháº­n (PENDING).");
        }

        ticket.setStatus(TicketStatus.CANCELLED.getValue());
        ticketRepository.save(ticket);

        addStatusHistory(ticket.getId(), TicketStatus.PENDING.getValue(), TicketStatus.CANCELLED.getValue(), currentUserId, "KhÃ¡ch hÃ ng chá»§ Ä‘á»™ng há»§y yÃªu cáº§u");

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse reviewTicket(Long id, ReviewTicketRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u há»— trá»£ ID: " + id));

        if (!TicketStatus.PENDING.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Phiáº¿u khÃ´ng á»Ÿ tráº¡ng thÃ¡i Chá» tiáº¿p nháº­n.");
        }

        String action = request.getAction().toLowerCase();
        if ("reject".equals(action)) {
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lÃ²ng nháº­p lÃ½ do tá»« chá»‘i yÃªu cáº§u.");
            }
            ticket.setStatus(TicketStatus.REJECTED.getValue());
            ticket.setRejectReason(request.getRejectReason().trim());
            ticket.setReviewedBy(request.getStaffId());
            ticket.setReviewedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.PENDING.getValue(), TicketStatus.REJECTED.getValue(), request.getStaffId(),
                    "NhÃ¢n viÃªn tá»« chá»‘i sÆ¡ duyá»‡t: " + request.getRejectReason().trim());
        } else if ("approve".equals(action)) {
            String cause = request.getCause();
            if (cause == null || (!TicketCause.STORE_FAULT.getValue().equals(cause) && !TicketCause.CUSTOMER_PREFERENCE.getValue().equals(cause))) {
                throw new IllegalArgumentException("Vui lÃ²ng chá»n nguyÃªn nhÃ¢n lá»—i (STORE_FAULT hoáº·c CUSTOMER_PREFERENCE).");
            }
            ticket.setStatus(TicketStatus.AWAITING_RETURN.getValue());
            ticket.setCause(cause);
            ticket.setReviewedBy(request.getStaffId());
            ticket.setReviewedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.PENDING.getValue(), TicketStatus.AWAITING_RETURN.getValue(), request.getStaffId(),
                    "NhÃ¢n viÃªn cháº¥p thuáº­n tiáº¿p nháº­n. NguyÃªn nhÃ¢n: " + (TicketCause.STORE_FAULT.getValue().equals(cause) ? "Lá»—i cá»­a hÃ ng/NSX" : "Nhu cáº§u cÃ¡ nhÃ¢n"));
        } else {
            throw new IllegalArgumentException("Thao tÃ¡c sÆ¡ duyá»‡t khÃ´ng há»£p lá»‡ ('approve' hoáº·c 'reject').");
        }

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse inspectTicket(Long id, InspectionRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u há»— trá»£ ID: " + id));

        if (!TicketStatus.AWAITING_RETURN.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Phiáº¿u pháº£i á»Ÿ tráº¡ng thÃ¡i Chá» gá»­i láº¡i hÃ ng Ä‘á»ƒ tiáº¿n hÃ nh tháº©m Ä‘á»‹nh.");
        }

        String result = request.getResult().toUpperCase();
        if (!InspectionResult.PASSED.getValue().equals(result) && !InspectionResult.FAILED.getValue().equals(result)) {
            throw new IllegalArgumentException("Káº¿t quáº£ tháº©m Ä‘á»‹nh pháº£i lÃ  PASSED hoáº·c FAILED.");
        }

        TicketInspection inspection = TicketInspection.builder()
                .ticketId(ticket.getId())
                .staffId(request.getStaffId() != null ? request.getStaffId() : 0L)
                .result(result)
                .note(request.getNote())
                .build();
        inspectionRepository.save(inspection);

        if (InspectionResult.FAILED.getValue().equals(result)) {
            ticket.setStatus(TicketStatus.INSPECTION_FAILED.getValue());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.INSPECTION_FAILED.getValue(),
                    request.getStaffId(), "Tháº©m Ä‘á»‹nh KHÃ”NG Äáº T: " + (request.getNote() != null ? request.getNote() : "") + ". HÃ ng sáº½ Ä‘Æ°á»£c gá»­i tráº£ láº¡i khÃ¡ch.");
        } else {
            // PASSED
            // Nếu lý do là do khách không muốn/đổi ý (CUSTOMER_PREFERENCE) -> Từ chối phiếu (REJECTED), chỉ đổi trả khi lỗi từ phía cửa hàng (STORE_FAULT)
            if (TicketCause.CUSTOMER_PREFERENCE.getValue().equals(ticket.getCause())) {
                ticket.setStatus(TicketStatus.REJECTED.getValue());
                ticket.setRejectReason("Sản phẩm đạt chuẩn nhưng lý do là nhu cầu cá nhân/đổi ý. Cửa hàng chỉ giải quyết đổi trả khi lỗi từ phía cửa hàng.");
                ticketRepository.save(ticket);

                addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.REJECTED.getValue(),
                        request.getStaffId(), "Thẩm định ĐẠT chuẩn nhưng nguyên nhân là CUSTOMER_PREFERENCE (khách đổi ý). Cửa hàng từ chối yêu cầu, gửi trả lại hàng cũ.");
                return loadFullResponse(ticket);
            }

            if (TicketType.REFUND.getValue().equals(ticket.getTicketType())) {
                BigDecimal refundAmount = calculateRefundAmount(ticket);
                ticket.setRefundAmount(refundAmount);

                if (refundAmount.compareTo(APPROVAL_THRESHOLD) < 0) {
                    ticket.setStatus(TicketStatus.PENDING_TRANSFER.getValue());
                    ticket.setApprovedBy(request.getStaffId());
                    ticket.setApprovedAt(LocalDateTime.now());
                    ticketRepository.save(ticket);

                    addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.PENDING_TRANSFER.getValue(),
                            request.getStaffId(), "Tháº©m Ä‘á»‹nh Äáº T. HoÃ n tiá»n " + refundAmount + " VND (< 3 triá»‡u), NhÃ¢n viÃªn duyá»‡t chuyá»ƒn khoáº£n.");
                } else {
                    ticket.setStatus(TicketStatus.PENDING_APPROVAL.getValue());
                    ticketRepository.save(ticket);

                    addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.PENDING_APPROVAL.getValue(),
                            request.getStaffId(), "Tháº©m Ä‘á»‹nh Äáº T. HoÃ n tiá»n " + refundAmount + " VND (>= 3 triá»‡u), chuyá»ƒn Quáº£n lÃ½ phÃª duyá»‡t.");
                }
            } else {
                // EXCHANGE
                ticketRepository.save(ticket);
                addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.AWAITING_RETURN.getValue(),
                        request.getStaffId(), "Tháº©m Ä‘á»‹nh Äáº T. Chá» nhÃ¢n viÃªn xuáº¥t gá»­i hÃ ng thay tháº¿ hoáº·c kiá»ƒm tra tá»“n kho.");
            }
        }

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse approveRefund(Long id, Long staffId) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u há»— trá»£ ID: " + id));

        if (!TicketStatus.AWAITING_RETURN.getValue().equals(ticket.getStatus()) && !TicketStatus.PENDING_APPROVAL.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Tráº¡ng thÃ¡i hiá»‡n táº¡i khÃ´ng thá»ƒ duyá»‡t hoÃ n tiá»n.");
        }

        BigDecimal amount = ticket.getRefundAmount() != null ? ticket.getRefundAmount() : calculateRefundAmount(ticket);
        ticket.setRefundAmount(amount);

        if (amount.compareTo(APPROVAL_THRESHOLD) >= 0) {
            throw new IllegalStateException("Sá»‘ tiá»n hoÃ n tá»« 3.000.000Ä‘ trá»Ÿ lÃªn báº¯t buá»™c pháº£i do Quáº£n lÃ½ (Manager) phÃª duyá»‡t.");
        }

        ticket.setStatus(TicketStatus.PENDING_TRANSFER.getValue());
        ticket.setApprovedBy(staffId);
        ticket.setApprovedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.PENDING_TRANSFER.getValue(),
                staffId, "NhÃ¢n viÃªn phÃª duyá»‡t hoÃ n tiá»n: " + amount + " VND");

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse managerDecision(Long id, ManagerDecisionRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u há»— trá»£ ID: " + id));

        if (!TicketStatus.PENDING_APPROVAL.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Phiáº¿u khÃ´ng á»Ÿ tráº¡ng thÃ¡i Chá» Quáº£n lÃ½ phÃª duyá»‡t (PENDING_APPROVAL).");
        }

        String decision = request.getDecision().toLowerCase();
        if ("reject".equals(decision)) {
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Quáº£n lÃ½ tá»« chá»‘i báº¯t buá»™c pháº£i nháº­p lÃ½ do.");
            }
            ticket.setStatus(TicketStatus.REJECTED.getValue());
            ticket.setRejectReason(request.getRejectReason().trim());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.PENDING_APPROVAL.getValue(), TicketStatus.REJECTED.getValue(),
                    request.getManagerId(), "Quáº£n lÃ½ tá»« chá»‘i hoÃ n tiá»n: " + request.getRejectReason().trim());
        } else if ("approve".equals(decision)) {
            ticket.setStatus(TicketStatus.PENDING_TRANSFER.getValue());
            ticket.setApprovedBy(request.getManagerId());
            ticket.setApprovedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.PENDING_APPROVAL.getValue(), TicketStatus.PENDING_TRANSFER.getValue(),
                    request.getManagerId(), "Quáº£n lÃ½ Ä‘Ã£ phÃª duyá»‡t khoáº£n hoÃ n tiá»n: " + ticket.getRefundAmount() + " VND");
        } else {
            throw new IllegalArgumentException("Quyáº¿t Ä‘á»‹nh quáº£n lÃ½ khÃ´ng há»£p lá»‡ ('approve' hoáº·c 'reject').");
        }

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse shipExchange(Long id, ShipExchangeRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u há»— trá»£ ID: " + id));

        if (!TicketType.EXCHANGE.getValue().equals(ticket.getTicketType())) {
            throw new IllegalStateException("Chá»‰ Ã¡p dá»¥ng cho phiáº¿u Äá»•i sáº£n pháº©m (EXCHANGE).");
        }

        ticket.setStatus(TicketStatus.COMPLETED.getValue());
        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        String note = "ÄÃ£ xuáº¥t kho gá»­i hÃ ng thay tháº¿ cho khÃ¡ch. MÃ£ váº­n Ä‘Æ¡n: " + (request.getTrackingCode() != null ? request.getTrackingCode() : "ChÆ°a cÃ³");
        addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.COMPLETED.getValue(),
                request.getStaffId(), note);

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse replacementUnavailable(Long id, Long staffId, String note) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hỗ trợ ID: " + id));

        if (!TicketType.EXCHANGE.getValue().equals(ticket.getTicketType())) {
            throw new IllegalStateException("Chỉ áp dụng cho phiếu Đổi sản phẩm (EXCHANGE).");
        }

        // Tự động gửi thông báo hết hàng và hoàn tiền cho khách (không cần customer confirm)
        BigDecimal refundAmount = calculateRefundAmount(ticket);
        ticket.setRefundAmount(refundAmount);

        String autoRefundNote = "Sản phẩm thay thế trong kho đã hết hàng. Hệ thống gửi thông báo hết hàng và tự động chuyển sang hoàn tiền " 
                + refundAmount + " VND cho khách (không cần khách xác nhận).";
        if (note != null && !note.isBlank()) {
            autoRefundNote += " Ghi chú: " + note;
        }

        if (refundAmount.compareTo(APPROVAL_THRESHOLD) < 0) {
            ticket.setStatus(TicketStatus.PENDING_TRANSFER.getValue());
            ticket.setApprovedBy(staffId);
            ticket.setApprovedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.PENDING_TRANSFER.getValue(),
                    staffId, autoRefundNote + " Nhân viên chuyển tiền qua QR.");
        } else {
            ticket.setStatus(TicketStatus.PENDING_APPROVAL.getValue());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.PENDING_APPROVAL.getValue(),
                    staffId, autoRefundNote + " Chờ Quản lý phê duyệt.");
        }

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse recordRefundTransfer(Long id, RefundTransferRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u há»— trá»£ ID: " + id));

        if (!TicketStatus.PENDING_TRANSFER.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Phiáº¿u khÃ´ng á»Ÿ tráº¡ng thÃ¡i Chá» chuyá»ƒn khoáº£n hoÃ n tiá»n (PENDING_TRANSFER).");
        }

        String ref = request.getBankReference().trim();
        if (refundRecordRepository.existsByBankReference(ref)) {
            throw new IllegalStateException("MÃ£ tham chiáº¿u ngÃ¢n hÃ ng nÃ y Ä‘Ã£ tá»“n táº¡i trong há»‡ thá»‘ng.");
        }

        Optional<RefundRecord> existing = refundRecordRepository.findByTicketId(ticket.getId());
        if (existing.isPresent()) {
            throw new IllegalStateException("Phiáº¿u nÃ y Ä‘Ã£ Ä‘Æ°á»£c ghi nháº­n hoÃ n tiá»n.");
        }

        Order order = orderRepository.findById(ticket.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n hÃ ng ID: " + ticket.getOrderId()));

        RefundRecord record = RefundRecord.builder()
                .ticketId(ticket.getId())
                .paymentId(null)
                .orderId(order.getId())
                .amount(ticket.getRefundAmount())
                .bankReference(ref)
                .transferredBy(request.getStaffId() != null ? request.getStaffId() : 0L)
                .note(request.getNote())
                .build();
        refundRecordRepository.save(record);

        if (ticket.getRefundAmount().compareTo(order.getTotalAmount()) >= 0) {
            order.setPaymentStatus("refunded");
        } else {
            order.setPaymentStatus("partially_refunded");
        }
        orderRepository.save(order);

        ticket.setStatus(TicketStatus.REFUNDED.getValue());
        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        addStatusHistory(ticket.getId(), TicketStatus.PENDING_TRANSFER.getValue(), TicketStatus.REFUNDED.getValue(),
                request.getStaffId(), "NhÃ¢n viÃªn Ä‘Ã£ chuyá»ƒn khoáº£n thÃ nh cÃ´ng " + ticket.getRefundAmount() + " VND. MÃ£ GD: " + ref);

        return loadFullResponse(ticket);
    }

    private BigDecimal calculateRefundAmount(SupportTicket ticket) {
        List<SupportTicketItem> items = itemRepository.findByTicketId(ticket.getId());
        Order order = orderRepository.findById(ticket.getOrderId()).orElse(null);
        List<OrderItem> orderItems = order != null ? orderItemRepository.findByOrderId(order.getId()) : Collections.emptyList();
        Map<Long, OrderItem> orderItemMap = orderItems.stream().collect(Collectors.toMap(OrderItem::getId, oi -> oi));

        BigDecimal sum = BigDecimal.ZERO;
        for (SupportTicketItem it : items) {
            OrderItem oi = orderItemMap.get(it.getOrderItemId());
            if (oi != null && oi.getUnitPrice() != null) {
                sum = sum.add(oi.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            }
        }

        // HoÃ n phÃ­ ship náº¿u lá»—i do cá»­a hÃ ng
        if (TicketCause.STORE_FAULT.getValue().equals(ticket.getCause()) && order != null && order.getShippingFee() != null) {
            sum = sum.add(order.getShippingFee());
        }

        return sum;
    }

    private void addStatusHistory(Long ticketId, String fromStatus, String toStatus, Long actorId, String note) {
        TicketStatusHistory history = TicketStatusHistory.builder()
                .ticketId(ticketId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actorId(actorId)
                .note(note)
                .build();
        statusHistoryRepository.save(history);
    }

    private TicketResponse loadFullResponse(SupportTicket ticket) {
        List<SupportTicketItem> items = itemRepository.findByTicketId(ticket.getId());
        List<TicketEvidence> evidences = evidenceRepository.findByTicketId(ticket.getId());
        List<TicketStatusHistory> histories = statusHistoryRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
        TicketInspection inspection = inspectionRepository.findByTicketId(ticket.getId()).orElse(null);
        RefundRecord refundRecord = refundRecordRepository.findByTicketId(ticket.getId()).orElse(null);
        Order order = orderRepository.findById(ticket.getOrderId()).orElse(null);

        return buildResponse(ticket, items, evidences, inspection, refundRecord, histories, order);
    }

    private TicketResponse buildResponse(
            SupportTicket ticket,
            List<SupportTicketItem> items,
            List<TicketEvidence> evidences,
            TicketInspection inspection,
            RefundRecord refundRecord,
            List<TicketStatusHistory> histories,
            Order order
    ) {
        List<OrderItem> orderItems = order != null ? orderItemRepository.findByOrderId(order.getId()) : Collections.emptyList();
        Map<Long, OrderItem> oiMap = orderItems.stream().collect(Collectors.toMap(OrderItem::getId, i -> i));

        List<TicketItemResponse> itemResponses = items.stream().map(it -> {
            OrderItem oi = oiMap.get(it.getOrderItemId());
            ProductVariant replacement = it.getReplacementVariantId() != null
                    ? productVariantRepository.findById(it.getReplacementVariantId()).orElse(null)
                    : null;

            return TicketItemResponse.builder()
                    .id(it.getId())
                    .orderItemId(it.getOrderItemId())
                    .productName(oi != null ? oi.getProductName() : null)
                    .variantInfo(oi != null ? oi.getVariantInfo() : null)
                    .unitPrice(oi != null ? oi.getUnitPrice() : null)
                    .quantity(it.getQuantity())
                    .replacementVariantId(it.getReplacementVariantId())
                    .replacementVariantSku(replacement != null ? replacement.getSku() : null)
                    .replacementVariantInfo(replacement != null ? (replacement.getColor() + " / " + replacement.getSize()) : null)
                    .build();
        }).collect(Collectors.toList());

        List<String> evidenceUrls = evidences.stream().map(TicketEvidence::getFileUrl).collect(Collectors.toList());

        TicketInspectionResponse inspectionResponse = inspection != null ? TicketInspectionResponse.builder()
                .id(inspection.getId())
                .staffId(inspection.getStaffId())
                .result(inspection.getResult())
                .note(inspection.getNote())
                .inspectedAt(inspection.getInspectedAt())
                .build() : null;

        RefundRecordResponse refundRecordResponse = refundRecord != null ? RefundRecordResponse.builder()
                .id(refundRecord.getId())
                .ticketId(refundRecord.getTicketId())
                .orderId(refundRecord.getOrderId())
                .amount(refundRecord.getAmount())
                .bankReference(refundRecord.getBankReference())
                .transferredBy(refundRecord.getTransferredBy())
                .note(refundRecord.getNote())
                .transferredAt(refundRecord.getTransferredAt())
                .build() : null;

        List<TicketStatusHistoryResponse> historyResponses = histories != null ? histories.stream().map(h ->
                TicketStatusHistoryResponse.builder()
                        .id(h.getId())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .actorId(h.getActorId())
                        .note(h.getNote())
                        .createdAt(h.getCreatedAt())
                        .build()
        ).collect(Collectors.toList()) : Collections.emptyList();

        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .userId(ticket.getUserId())
                .orderId(ticket.getOrderId())
                .orderCode(order != null ? order.getOrderCode() : null)
                .ticketType(ticket.getTicketType())
                .status(ticket.getStatus())
                .reason(ticket.getReason())
                .description(ticket.getDescription())
                .cause(ticket.getCause())
                .rejectReason(ticket.getRejectReason())
                .refundAmount(ticket.getRefundAmount())
                .refundQrUrl(ticket.getRefundQrUrl())
                .reviewedBy(ticket.getReviewedBy())
                .reviewedAt(ticket.getReviewedAt())
                .approvedBy(ticket.getApprovedBy())
                .approvedAt(ticket.getApprovedAt())
                .resolvedAt(ticket.getResolvedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .items(itemResponses)
                .evidenceUrls(evidenceUrls)
                .inspection(inspectionResponse)
                .refundRecord(refundRecordResponse)
                .statusHistories(historyResponses)
                .build();
    }
}

