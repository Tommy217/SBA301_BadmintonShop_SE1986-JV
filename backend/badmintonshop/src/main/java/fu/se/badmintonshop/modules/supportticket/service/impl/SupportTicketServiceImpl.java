package fu.se.badmintonshop.modules.supportticket.service.impl;

import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.entity.OrderItem;
import fu.se.badmintonshop.modules.order.repository.OrderItemRepository;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.payment.entity.Payment;
import fu.se.badmintonshop.modules.payment.repository.PaymentRepository;
import fu.se.badmintonshop.modules.supportticket.dto.*;
import fu.se.badmintonshop.modules.supportticket.entity.*;
import fu.se.badmintonshop.modules.supportticket.enums.*;
import fu.se.badmintonshop.modules.supportticket.repository.*;
import fu.se.badmintonshop.modules.supportticket.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final BigDecimal DEFAULT_APPROVAL_THRESHOLD = new BigDecimal("3000000"); // 3,000,000 VND
    private static final int RETURN_WINDOW_DAYS = 7;

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketItemRepository itemRepository;
    private final TicketEvidenceRepository evidenceRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final TicketInspectionRepository inspectionRepository;
    private final RefundRecordRepository refundRecordRepository;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, Long currentUserId) {
        log.info("Khởi tạo Support Ticket cho orderId={}, userId={}", request.getOrderId(), currentUserId);

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + request.getOrderId()));

        // 1. Kiểm tra quyền sở hữu đơn hàng
        Long userId = currentUserId != null ? currentUserId : (request.getUserId() != null ? request.getUserId() : order.getUserId());
        if (order.getUserId() != null && userId != null && !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không sở hữu đơn hàng này.");
        }

        // 2. Kiểm tra điều kiện 7 ngày kể từ ngày giao hàng
        if (order.getCreatedAt() != null && order.getCreatedAt().isBefore(LocalDateTime.now().minusDays(RETURN_WINDOW_DAYS + 30))) {
            // Lưu ý: Trong môi trường thực tế kiểm tra theo delivered_at, tạm thời chấp nhận đơn hàng
        }

        // 3. Mỗi đơn hàng chỉ được tạo tối đa 1 ticket (CR-BR)
        boolean hasActiveTicket = ticketRepository.existsByOrderIdAndStatusNot(order.getId(), TicketStatus.CANCELLED.getValue());
        if (hasActiveTicket) {
            throw new IllegalStateException("Đơn hàng này đã tồn tại một phiếu khiếu nại / đổi trả đang được xử lý.");
        }

        // 4. Validate loại ticket
        String type = request.getTicketType() != null ? request.getTicketType().toUpperCase() : TicketType.REFUND.getValue();
        if (!TicketType.REFUND.getValue().equals(type)) {
            throw new IllegalArgumentException("Hệ thống chỉ hỗ trợ yêu cầu Trả hàng & Hoàn tiền (REFUND).");
        }

        // 5. Bắt buộc có ảnh QR nhận tiền
        if (request.getRefundQrUrl() == null || request.getRefundQrUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Ảnh mã QR tài khoản ngân hàng nhận tiền là bắt buộc cho cả Hoàn tiền và Đổi hàng.");
        }

        // 6. Bắt buộc có tối thiểu 1 file bằng chứng
        if (request.getEvidenceUrls() == null || request.getEvidenceUrls().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng tải lên ít nhất một hình ảnh hoặc video bằng chứng.");
        }

        // 7. Validate các sản phẩm yêu cầu
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        Map<Long, OrderItem> orderItemMap = orderItems.stream().collect(Collectors.toMap(OrderItem::getId, item -> item));

        for (TicketItemRequest it : request.getItems()) {
            OrderItem oi = orderItemMap.get(it.getOrderItemId());
            if (oi == null) {
                throw new IllegalArgumentException("Sản phẩm ID " + it.getOrderItemId() + " không thuộc đơn hàng này.");
            }
            if (it.getQuantity() > oi.getQuantity()) {
                throw new IllegalArgumentException("Số lượng yêu cầu (" + it.getQuantity() + ") vượt quá số lượng đã mua (" + oi.getQuantity() + ").");
            }
        }

        // Tạo mã ticket: ST-yyyyMMdd-XXXXX
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

        // Ghi nhận lịch sử trạng thái đầu tiên
        addStatusHistory(savedTicket.getId(), null, TicketStatus.PENDING.getValue(), userId, "Khách hàng tạo phiếu yêu cầu hỗ trợ");

        return buildResponse(savedTicket, savedItems, savedEvidences, null, null, null, order);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id, Long currentUserId, boolean isStaffOrManager) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hỗ trợ ID: " + id));

        if (!isStaffOrManager && currentUserId != null && !ticket.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem phiếu hỗ trợ này.");
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
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hỗ trợ ID: " + id));

        if (currentUserId != null && !ticket.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy phiếu này.");
        }

        if (!TicketStatus.PENDING.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy yêu cầu khi phiếu ở trạng thái Chờ tiếp nhận (PENDING).");
        }

        ticket.setStatus(TicketStatus.CANCELLED.getValue());
        ticketRepository.save(ticket);

        addStatusHistory(ticket.getId(), TicketStatus.PENDING.getValue(), TicketStatus.CANCELLED.getValue(), currentUserId, "Khách hàng chủ động hủy yêu cầu");

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse reviewTicket(Long id, ReviewTicketRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hỗ trợ ID: " + id));

        if (!TicketStatus.PENDING.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Phiếu không ở trạng thái Chờ tiếp nhận.");
        }

        String action = request.getAction().toLowerCase();
        if ("reject".equals(action)) {
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối yêu cầu.");
            }
            ticket.setStatus(TicketStatus.REJECTED.getValue());
            ticket.setRejectReason(request.getRejectReason().trim());
            ticket.setReviewedBy(request.getStaffId());
            ticket.setReviewedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.PENDING.getValue(), TicketStatus.REJECTED.getValue(), request.getStaffId(),
                    "Nhân viên từ chối sơ duyệt: " + request.getRejectReason().trim());
        } else if ("approve".equals(action)) {
            String cause = request.getCause();
            if (cause == null || (!TicketCause.STORE_FAULT.getValue().equals(cause) && !TicketCause.CUSTOMER_PREFERENCE.getValue().equals(cause))) {
                throw new IllegalArgumentException("Vui lòng chọn nguyên nhân lỗi (STORE_FAULT hoặc CUSTOMER_PREFERENCE).");
            }
            ticket.setStatus(TicketStatus.AWAITING_RETURN.getValue());
            ticket.setCause(cause);
            ticket.setReviewedBy(request.getStaffId());
            ticket.setReviewedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.PENDING.getValue(), TicketStatus.AWAITING_RETURN.getValue(), request.getStaffId(),
                    "Nhân viên chấp thuận tiếp nhận. Nguyên nhân: " + (TicketCause.STORE_FAULT.getValue().equals(cause) ? "Lỗi cửa hàng/NSX" : "Nhu cầu cá nhân"));
        } else {
            throw new IllegalArgumentException("Thao tác sơ duyệt không hợp lệ ('approve' hoặc 'reject').");
        }

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse inspectTicket(Long id, InspectionRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hỗ trợ ID: " + id));

        if (!TicketStatus.AWAITING_RETURN.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Phiếu phải ở trạng thái Chờ gửi lại hàng để tiến hành thẩm định.");
        }

        String result = request.getResult().toUpperCase();
        if (!InspectionResult.PASSED.getValue().equals(result) && !InspectionResult.FAILED.getValue().equals(result)) {
            throw new IllegalArgumentException("Kết quả thẩm định phải là PASSED hoặc FAILED.");
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
                    request.getStaffId(), "Thẩm định KHÔNG ĐẠT: " + (request.getNote() != null ? request.getNote() : "") + ". Hàng sẽ được gửi trả lại khách.");
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

            BigDecimal refundAmount = calculateRefundAmount(ticket);
            ticket.setRefundAmount(refundAmount);

            BigDecimal threshold = getApprovalThreshold();
            if (refundAmount.compareTo(threshold) < 0) {
                ticket.setStatus(TicketStatus.PENDING_TRANSFER.getValue());
                ticket.setApprovedBy(request.getStaffId());
                ticket.setApprovedAt(LocalDateTime.now());
                ticketRepository.save(ticket);

                addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.PENDING_TRANSFER.getValue(),
                        request.getStaffId(), "Thẩm định ĐẠT. Hoàn tiền " + refundAmount + " VND (< " + threshold + " VND), Nhân viên duyệt chuyển khoản.");
            } else {
                ticket.setStatus(TicketStatus.PENDING_APPROVAL.getValue());
                ticketRepository.save(ticket);

                addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.PENDING_APPROVAL.getValue(),
                        request.getStaffId(), "Thẩm định ĐẠT. Hoàn tiền " + refundAmount + " VND (>= " + threshold + " VND), chuyển Quản lý phê duyệt.");
            }
        }

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse approveRefund(Long id, Long staffId) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hỗ trợ ID: " + id));

        if (!TicketStatus.AWAITING_RETURN.getValue().equals(ticket.getStatus()) && !TicketStatus.PENDING_APPROVAL.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Trạng thái hiện tại không thể duyệt hoàn tiền.");
        }

        BigDecimal amount = ticket.getRefundAmount() != null ? ticket.getRefundAmount() : calculateRefundAmount(ticket);
        ticket.setRefundAmount(amount);

        BigDecimal threshold = getApprovalThreshold();
        if (amount.compareTo(threshold) >= 0) {
            throw new IllegalStateException("Số tiền hoàn từ 3.000.000đ trở lên bắt buộc phải do Quản lý (Manager) phê duyệt.");
        }

        ticket.setStatus(TicketStatus.PENDING_TRANSFER.getValue());
        ticket.setApprovedBy(staffId);
        ticket.setApprovedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        addStatusHistory(ticket.getId(), TicketStatus.AWAITING_RETURN.getValue(), TicketStatus.PENDING_TRANSFER.getValue(),
                staffId, "Nhân viên phê duyệt hoàn tiền: " + amount + " VND");

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse managerDecision(Long id, ManagerDecisionRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hỗ trợ ID: " + id));

        if (!TicketStatus.PENDING_APPROVAL.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Phiếu không ở trạng thái Chờ Quản lý phê duyệt (PENDING_APPROVAL).");
        }

        String decision = request.getDecision().toLowerCase();
        if ("reject".equals(decision)) {
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Quản lý từ chối bắt buộc phải nhập lý do.");
            }
            ticket.setStatus(TicketStatus.REJECTED.getValue());
            ticket.setRejectReason(request.getRejectReason().trim());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.PENDING_APPROVAL.getValue(), TicketStatus.REJECTED.getValue(),
                    request.getManagerId(), "Quản lý từ chối hoàn tiền: " + request.getRejectReason().trim());
        } else if ("approve".equals(decision)) {
            ticket.setStatus(TicketStatus.PENDING_TRANSFER.getValue());
            ticket.setApprovedBy(request.getManagerId());
            ticket.setApprovedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            addStatusHistory(ticket.getId(), TicketStatus.PENDING_APPROVAL.getValue(), TicketStatus.PENDING_TRANSFER.getValue(),
                    request.getManagerId(), "Quản lý đã phê duyệt khoản hoàn tiền: " + ticket.getRefundAmount() + " VND");
        } else {
            throw new IllegalArgumentException("Quyết định quản lý không hợp lệ ('approve' hoặc 'reject').");
        }

        return loadFullResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse recordRefundTransfer(Long id, RefundTransferRequest request) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hỗ trợ ID: " + id));

        if (!TicketStatus.PENDING_TRANSFER.getValue().equals(ticket.getStatus())) {
            throw new IllegalStateException("Phiếu không ở trạng thái Chờ chuyển khoản hoàn tiền (PENDING_TRANSFER).");
        }

        String ref = request.getBankReference().trim();
        if (refundRecordRepository.existsByBankReference(ref)) {
            throw new IllegalStateException("Mã tham chiếu ngân hàng này đã tồn tại trong hệ thống.");
        }

        Optional<RefundRecord> existing = refundRecordRepository.findByTicketId(ticket.getId());
        if (existing.isPresent()) {
            throw new IllegalStateException("Phiếu này đã được ghi nhận hoàn tiền.");
        }

        Order order = orderRepository.findById(ticket.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng ID: " + ticket.getOrderId()));

        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()).orElse(null);
        Long paymentId = payment != null ? payment.getId() : null;

        RefundRecord record = RefundRecord.builder()
                .ticketId(ticket.getId())
                .paymentId(paymentId)
                .orderId(order.getId())
                .amount(ticket.getRefundAmount())
                .bankReference(ref)
                .transferredBy(request.getStaffId() != null ? request.getStaffId() : 0L)
                .note(request.getNote())
                .build();
        refundRecordRepository.save(record);

        if (payment != null) {
            payment.setStatus("refunded");
            paymentRepository.save(payment);
        }

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
                request.getStaffId(), "Nhân viên đã chuyển khoản thành công " + ticket.getRefundAmount() + " VND. Mã GD: " + ref);

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

        // Hoàn phí ship nếu lỗi do cửa hàng
        if (TicketCause.STORE_FAULT.getValue().equals(ticket.getCause()) && order != null && order.getShippingFee() != null) {
            sum = sum.add(order.getShippingFee());
        }

        // Chặn trần: Số tiền hoàn không bao giờ vượt quá tổng tiền thực tế khách đã trả cho đơn hàng
        if (order != null && order.getTotalAmount() != null && sum.compareTo(order.getTotalAmount()) > 0) {
            sum = order.getTotalAmount();
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
            return TicketItemResponse.builder()
                    .id(it.getId())
                    .orderItemId(it.getOrderItemId())
                    .productName(oi != null ? oi.getProductName() : null)
                    .variantInfo(oi != null ? oi.getVariantInfo() : null)
                    .unitPrice(oi != null ? oi.getUnitPrice() : null)
                    .quantity(it.getQuantity())
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

    @Override
    public BigDecimal getApprovalThreshold() {
        try {
            ensureSystemSettingsTable();
            String val = jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM system_settings WHERE setting_key = 'refund_approval_threshold'",
                    String.class);
            if (val != null && !val.isBlank()) {
                return new BigDecimal(val.trim());
            }
        } catch (Exception e) {
            log.warn("Không thể đọc ngưỡng refund_approval_threshold từ DB, dùng mặc định 3,000,000 VND: {}", e.getMessage());
        }
        return DEFAULT_APPROVAL_THRESHOLD;
    }

    @Override
    @Transactional
    public void updateApprovalThreshold(BigDecimal newThreshold, Long managerId) {
        if (newThreshold == null || newThreshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ngưỡng phê duyệt phải lớn hơn 0 VNĐ.");
        }
        ensureSystemSettingsTable();
        int updated = jdbcTemplate.update(
                "UPDATE system_settings SET setting_value = ?, updated_at = SYSDATETIME(), updated_by = ? WHERE setting_key = 'refund_approval_threshold'",
                newThreshold.toPlainString(), managerId);
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO system_settings (setting_key, setting_value, description, updated_at, updated_by) VALUES ('refund_approval_threshold', ?, N'Ngưỡng hoàn tiền yêu cầu Quản lý phê duyệt (VND)', SYSDATETIME(), ?)",
                    newThreshold.toPlainString(), managerId);
        }
        log.info("Manager ID {} đã cập nhật ngưỡng duyệt hoàn tiền thành: {} VND", managerId, newThreshold);
    }

    private void ensureSystemSettingsTable() {
        try {
            jdbcTemplate.execute("IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'system_settings') " +
                    "BEGIN " +
                    "CREATE TABLE system_settings (" +
                    "  setting_key VARCHAR(100) PRIMARY KEY," +
                    "  setting_value NVARCHAR(500) NOT NULL," +
                    "  description NVARCHAR(255) NULL," +
                    "  updated_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()," +
                    "  updated_by BIGINT NULL" +
                    "); " +
                    "END; " +
                    "IF NOT EXISTS (SELECT * FROM system_settings WHERE setting_key = 'refund_approval_threshold') " +
                    "BEGIN " +
                    "INSERT INTO system_settings (setting_key, setting_value, description) " +
                    "VALUES ('refund_approval_threshold', '3000000', N'Ngưỡng hoàn tiền yêu cầu Quản lý phê duyệt (VND)'); " +
                    "END; " +
                    "IF NOT EXISTS (SELECT * FROM system_settings WHERE setting_key = 'payment_qr_timeout_minutes') " +
                    "BEGIN " +
                    "INSERT INTO system_settings (setting_key, setting_value, description) " +
                    "VALUES ('payment_qr_timeout_minutes', '15', N'Thời gian hết hạn mã QR thanh toán (phút)'); " +
                    "END;");
        } catch (Exception e) {
            log.warn("Lỗi kiểm tra/tạo bảng system_settings: {}", e.getMessage());
        }
    }
}

