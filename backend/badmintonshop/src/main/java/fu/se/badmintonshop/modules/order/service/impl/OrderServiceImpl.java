package fu.se.badmintonshop.modules.order.service.impl;

import static fu.se.badmintonshop.modules.common.mapper.SnapshotMapper.*;
import static fu.se.badmintonshop.modules.common.service.CommerceValidation.*;

import fu.se.badmintonshop.modules.auth.service.AuthService;
import fu.se.badmintonshop.modules.cart.repository.CartItemRepository;
import fu.se.badmintonshop.modules.catalog.entity.*;
import fu.se.badmintonshop.modules.catalog.repository.ProductRepository;
import fu.se.badmintonshop.modules.catalog.repository.ProductVariantRepository;
import fu.se.badmintonshop.modules.catalog.repository.VoucherRepository;
import fu.se.badmintonshop.modules.catalog.repository.VoucherScopeRepository;
import fu.se.badmintonshop.modules.catalog.repository.VoucherUsageRepository;
import fu.se.badmintonshop.modules.common.service.CommerceValidation;
import fu.se.badmintonshop.modules.common.service.EntityLockService;
import fu.se.badmintonshop.modules.delivery.repository.ShipmentRepository;
import fu.se.badmintonshop.modules.inventory.entity.InventoryMovement;
import fu.se.badmintonshop.modules.inventory.repository.InventoryMovementRepository;
import fu.se.badmintonshop.modules.order.dto.request.*;
import fu.se.badmintonshop.modules.order.entity.*;
import fu.se.badmintonshop.modules.order.exception.OrderException;
import fu.se.badmintonshop.modules.order.repository.OrderItemRepository;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import fu.se.badmintonshop.modules.order.repository.OrderStatusHistoryRepository;
import fu.se.badmintonshop.modules.order.service.OrderService;
import fu.se.badmintonshop.modules.payment.dto.request.CreatePaymentRequest;
import fu.se.badmintonshop.modules.payment.entity.Payment;
import fu.se.badmintonshop.modules.payment.repository.PaymentRepository;
import fu.se.badmintonshop.modules.payment.service.PaymentService;
import fu.se.badmintonshop.modules.supportticket.repository.RefundRecordRepository;
import fu.se.badmintonshop.modules.supportticket.repository.SupportTicketItemRepository;
import fu.se.badmintonshop.modules.supportticket.repository.SupportTicketRepository;
import fu.se.badmintonshop.modules.supportticket.repository.TicketEvidenceRepository;
import fu.se.badmintonshop.modules.supportticket.repository.TicketInspectionRepository;
import fu.se.badmintonshop.modules.supportticket.repository.TicketStatusHistoryRepository;
import fu.se.badmintonshop.modules.supportticket.service.SupportActions;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Transactional JPA checkout and order lifecycle service. */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final ProductVariantRepository productVariantRepository;
    private final CartItemRepository cartItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final VoucherScopeRepository voucherScopeRepository;
    private final ProductRepository productRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final VoucherRepository voucherRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SupportTicketItemRepository supportTicketItemRepository;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepository;
    private final TicketInspectionRepository ticketInspectionRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final TicketEvidenceRepository ticketEvidenceRepository;
    private final EntityLockService locks;
    private final SupportActions support;
    private final PaymentService payments;

    @Value("${shop.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final Map<String, Set<String>> TRANSITIONS =
            Map.of(
                    "pending",
                    Set.of("confirmed", "cancelled"),
                    "confirmed",
                    Set.of("preparing", "cancelled"),
                    "preparing",
                    Set.of("cancelled"),
                    "shipping",
                    Set.of(),
                    "completed",
                    Set.of("returned"),
                    "cancelled",
                    Set.of(),
                    "returned",
                    Set.of());

    @Transactional
    public Map<String, Object> snapshot(AuthService.User user) {
        return snapshot(user, management(user));
    }

    @Transactional
    public Map<String, Object> customerSnapshot(AuthService.User user) {
        return snapshot(user, false);
    }

    private Map<String, Object> snapshot(AuthService.User user, boolean staff) {
        if (user != null) {
            try {
                payments.syncPendingPayments();
            } catch (Exception ignored) {
            }
        }
        var result = new LinkedHashMap<String, Object>();
        for (String name :
                List.of(
                        "orders",
                        "order_items",
                        "payments",
                        "order_status_history",
                        "support_tickets",
                        "ticket_items",
                        "ticket_status_history",
                        "ticket_inspections",
                        "refund_records")) {
            result.put(name, new ArrayList<>());
        }
        if (user == null) return result;

        var orderEntities =
                staff
                        ? orderRepository.findAllByOrderById()
                        : orderRepository.findByUserIdOrderById(user.id());
        var orderIds = orderEntities.stream().map(Order::getId).toList();
        var orderRows = snapshots(orderEntities);
        for (var order : orderRows) {
            long orderId = id(order.get("id"));
            order.put(
                    "can_cancel",
                    Set.of("pending", "confirmed", "preparing").contains(order.get("status"))
                            && Set.of("unpaid", "paid").contains(order.get("payment_status"))
                            && shipmentRepository.countEffectiveShipments(orderId) == 0
                            && supportTicketRepository.countCancellationBlockers(orderId) == 0);
        }
        result.put("orders", orderRows);
        result.put(
                "order_items", snapshots(orderItemRepository.findByOrderIdInOrderById(orderIds)));
        result.put("payments", snapshots(paymentRepository.findByOrderIdInOrderById(orderIds)));
        result.put(
                "order_status_history",
                snapshots(orderStatusHistoryRepository.findByOrderIdInOrderById(orderIds)));
        result.put(
                "support_tickets",
                snapshots(supportTicketRepository.findByOrderIdInOrderById(orderIds)));
        var tickets = supportTicketRepository.findByOrderIdInOrderById(orderIds);
        var ticketRows = new ArrayList<Map<String, Object>>();
        for (var ticket : tickets) {
            var row = toRow(ticket);
            row.put(
                    "evidence_urls",
                    ticketEvidenceRepository.findTicketDetails(ticket.getId()).stream()
                            .map(e -> e.getFileUrl())
                            .toList());
            row.put(
                    "inspection",
                    snapshots(ticketInspectionRepository.findTicketDetails(ticket.getId())).stream()
                            .findFirst()
                            .orElse(null));
            row.put(
                    "refund_record",
                    snapshots(refundRecordRepository.findTicketDetails(ticket.getId())).stream()
                            .findFirst()
                            .orElse(null));
            ticketRows.add(row);
        }
        result.put("support_tickets", ticketRows);
        var ticketIds = tickets.stream().map(t -> t.getId()).toList();
        result.put(
                "ticket_items",
                snapshots(supportTicketItemRepository.findByTicketIdInOrderById(ticketIds)));
        result.put(
                "ticket_status_history",
                snapshots(ticketStatusHistoryRepository.findByTicketIdInOrderById(ticketIds)));
        result.put(
                "ticket_inspections",
                snapshots(ticketInspectionRepository.findByTicketIdInOrderById(ticketIds)));
        result.put(
                "refund_records",
                snapshots(refundRecordRepository.findByTicketIdInOrderById(ticketIds)));
        // van don doc qua API cua module delivery (/api/manager/shipments, /api/my/shipments)
        result.put("refund_approval_threshold", support.getTickets().getApprovalThreshold());
        result.put("payment_qr_timeout_minutes", payments.getPaymentTimeoutMinutes());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> voucherWallet(AuthService.User user) {
        require(user != null, HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        return Map.of(
                "vouchers", snapshots(voucherRepository.findWallet(user.id())),
                "voucher_usages", snapshots(voucherUsageRepository.findUserUsage(user.id())));
    }

    @Transactional
    public void execute(Map<String, Object> action, AuthService.User user) {
        require(user != null, HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        String type = text(action, "type");
        switch (type) {
            case "add", "update", "remove", "select_all" ->
                    throw new OrderException(HttpStatus.BAD_REQUEST, "Dùng API giỏ hàng.");
            case "checkout" -> checkout(action, user.id());
            case "manager_update_order" -> {
                require(
                        management(user),
                        HttpStatus.FORBIDDEN,
                        "Chỉ nhân viên hoặc quản lý được xử lý đơn.");
                updateOrder(action, user.id());
            }
            case "start_payment" -> startPayment(action, user);
            case "cancel_payment" -> cancelPayment(action, user);
            case "cancel_order" -> cancelOrder(action, user);
            default -> require(false, HttpStatus.BAD_REQUEST, "Thao tác không được hỗ trợ.");
        }
    }

    private void checkout(Map<String, Object> a, long userId) {
        String name = bounded(a, "receiver_name", 1, 150),
                phone = bounded(a, "receiver_phone", 1, 20);
        require(
                phone.matches("(0|\\+84)[0-9]{9}"),
                HttpStatus.BAD_REQUEST,
                "Số điện thoại không hợp lệ.");
        String address = bounded(a, "shipping_address", 10, 500), note = bounded(a, "note", 0, 500);
        String method = text(a, "payment_method");
        require(
                Set.of("cod", "prepaid").contains(method),
                HttpStatus.BAD_REQUEST,
                "Phương thức thanh toán không hợp lệ.");
        locks.lockUser(userId);
        List<Map<String, Object>> lines;
        if ("direct".equals(a.get("source"))) {
            lines =
                    List.of(
                            new LinkedHashMap<>(
                                    Map.of(
                                            "variant_id",
                                            id(a.get("variant_id")),
                                            "quantity",
                                            positiveInt(a.get("quantity")))));
        } else {
            require(
                    "cart".equals(a.get("source")),
                    HttpStatus.BAD_REQUEST,
                    "Nguồn đặt hàng không hợp lệ.");
            lines = snapshots(cartItemRepository.findOwnedItems(userId));
            if (a.containsKey("cart_item_ids")) {
                require(
                        a.get("cart_item_ids") instanceof List<?>,
                        HttpStatus.BAD_REQUEST,
                        "Danh sách giỏ hàng không hợp lệ.");
                var requested =
                        ((List<?>) a.get("cart_item_ids"))
                                .stream().map(OrderServiceImpl::id).toList();
                require(
                        !requested.isEmpty() && new HashSet<>(requested).size() == requested.size(),
                        HttpStatus.BAD_REQUEST,
                        "Chọn ít nhất một sản phẩm, không trùng lặp.");
                lines = lines.stream().filter(i -> requested.contains(id(i.get("id")))).toList();
                require(
                        lines.size() == requested.size(),
                        HttpStatus.CONFLICT,
                        "Sản phẩm không còn trong giỏ của bạn.");
            } else
                lines =
                        lines.stream()
                                .filter(i -> Boolean.TRUE.equals(i.get("is_selected")))
                                .toList();
        }
        require(!lines.isEmpty(), HttpStatus.BAD_REQUEST, "Chọn ít nhất một sản phẩm.");
        BigDecimal subtotal = BigDecimal.ZERO;
        // All orders acquire SKU locks in the same order; atomic deductions prevent overselling.
        lines = new ArrayList<>(lines);
        lines.sort(Comparator.comparingLong(i -> id(i.get("variant_id"))));
        for (var line : lines) {
            int quantity = positiveInt(line.get("quantity"));
            long variantId = id(line.get("variant_id"));
            require(
                    productVariantRepository.deductStock(quantity, variantId, quantity) == 1,
                    HttpStatus.CONFLICT,
                    "Sản phẩm không đủ tồn kho hoặc đã ngừng bán.");
            var v = variant(variantId);
            line.put("variant", v);
            subtotal = subtotal.add(money(v.get("price")).multiply(BigDecimal.valueOf(quantity)));
        }
        var voucher = applyVoucher(text(a, "voucher_code"), userId, lines, subtotal);
        BigDecimal discount = money(voucher.getOrDefault("discount", BigDecimal.ZERO));
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount).add(fee);
        require(
                total.compareTo(new BigDecimal("999999999999")) <= 0,
                HttpStatus.BAD_REQUEST,
                "Giá trị đơn hàng vượt giới hạn.");
        long orderId =
                orderRepository
                        .saveAndFlush(
                                Order.builder()
                                        .orderCode(
                                                "BS-"
                                                        + UUID.randomUUID()
                                                                .toString()
                                                                .replace("-", "")
                                                                .substring(0, 24))
                                        .userId(userId)
                                        .channel("online")
                                        .status("pending")
                                        .paymentStatus("unpaid")
                                        .receiverName(name)
                                        .receiverPhone(phone)
                                        .shippingAddress(address)
                                        .voucher(
                                                voucher.get("id") == null
                                                        ? null
                                                        : voucherRepository.getReferenceById(
                                                                Math.toIntExact(
                                                                        id(voucher.get("id")))))
                                        .voucherCode((String) voucher.get("code"))
                                        .subtotal(subtotal)
                                        .discountAmount(discount)
                                        .shippingFee(fee)
                                        .totalAmount(total)
                                        .note(note)
                                        .createdAt(now())
                                        .updatedAt(now())
                                        .build())
                        .getId();
        for (var line : lines) {
            @SuppressWarnings("unchecked")
            var v = (Map<String, Object>) line.get("variant");
            int quantity = positiveInt(line.get("quantity"));
            orderItemRepository.saveAndFlush(
                    OrderItem.builder()
                            .orderId(orderId)
                            .variantId(id(v.get("id")))
                            .productId(id(v.get("product_id")))
                            .productName((String) (v.get("name")))
                            .variantInfo(
                                    (String)
                                            (Objects.toString(v.get("color"), "")
                                                    + " / "
                                                    + Objects.toString(v.get("size"), "")))
                            .unitPrice(money(v.get("price")))
                            .quantity(quantity)
                            .lineTotal(money(v.get("price")).multiply(BigDecimal.valueOf(quantity)))
                            .build());
            movement(id(v.get("id")), -quantity, "stock_out", orderId, userId, "Đặt hàng");
            if (line.containsKey("id")) cartItemRepository.deleteById(id(line.get("id")));
        }
        paymentRepository.saveAndFlush(
                Payment.builder()
                        .gateway(null)
                        .orderId(orderId)
                        .method("cod".equals(method) ? "cod" : "qr")
                        .status("pending")
                        .amount(total)
                        .createdAt(now())
                        .updatedAt(now())
                        .build());
        history(orderId, null, "pending", userId, "Khách hàng đặt hàng");
        if (voucher.containsKey("id"))
            voucherUsageRepository.saveAndFlush(
                    VoucherUsage.builder()
                            .voucherId(Math.toIntExact(id(voucher.get("id"))))
                            .userId(userId)
                            .orderId(orderId)
                            .discountAmount(discount)
                            .usedAt(now())
                            .build());
    }

    private Map<String, Object> applyVoucher(
            String code, long userId, List<Map<String, Object>> lines, BigDecimal subtotal) {
        if (code.isBlank()) return Map.of();
        // The write locks this voucher until commit, also serializing per-user usage checks.
        require(
                voucherRepository.consumeAvailableCode(code.toUpperCase(Locale.ROOT), now(), now())
                        == 1,
                HttpStatus.CONFLICT,
                "Voucher không hợp lệ, hết hạn hoặc hết lượt sử dụng.");
        require(
                voucherRepository.countAccessibleCode(userId, code.toUpperCase(Locale.ROOT)) > 0,
                HttpStatus.FORBIDDEN,
                "Mã giảm giá chưa được cấp cho tài khoản của bạn.");
        var voucher =
                toRow(
                        voucherRepository
                                .findByUpperCode(code.toUpperCase(Locale.ROOT))
                                .orElseThrow(() -> notFound()));
        long voucherId = id(voucher.get("id"));
        require(
                voucherUsageRepository.countUserUsage(Math.toIntExact(voucherId), userId)
                        < number(voucher.get("per_user_limit")),
                HttpStatus.CONFLICT,
                "Bạn đã dùng hết lượt voucher.");
        require(
                subtotal.compareTo(money(voucher.get("min_order"))) >= 0,
                HttpStatus.BAD_REQUEST,
                "Đơn hàng chưa đạt giá trị tối thiểu của voucher.");
        var scopes =
                snapshots(voucherScopeRepository.findVoucherScopes(Math.toIntExact(voucherId)));
        BigDecimal eligible = BigDecimal.ZERO;
        String applies = Objects.toString(voucher.get("applies_to"));
        for (var line : lines) {
            @SuppressWarnings("unchecked")
            var v = (Map<String, Object>) line.get("variant");
            String key =
                    switch (applies) {
                        case "brand" -> "brand_id";
                        case "category" -> "category_id";
                        default -> "product_id";
                    };
            if ("all".equals(applies)
                    || scopes.stream()
                            .anyMatch(
                                    s ->
                                            applies.equals(s.get("scope_type"))
                                                    && id(s.get("scope_id")) == id(v.get(key)))) {
                eligible =
                        eligible.add(
                                money(v.get("price"))
                                        .multiply(
                                                BigDecimal.valueOf(
                                                        positiveInt(line.get("quantity")))));
            }
        }
        require(
                eligible.signum() > 0,
                HttpStatus.BAD_REQUEST,
                "Sản phẩm không thuộc phạm vi voucher.");
        BigDecimal discount = money(voucher.get("discount_value"));
        if ("percent".equals(voucher.get("discount_type")))
            discount =
                    eligible.multiply(discount).divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
        if (voucher.get("max_discount") != null)
            discount = discount.min(money(voucher.get("max_discount")));
        voucher.put("discount", discount.min(eligible));
        return voucher;
    }

    private void updateOrder(Map<String, Object> a, long actor) {
        long orderId = id(a.get("id"));
        locks.lockOrder(orderId);
        var o = toRow(orderRepository.findById(orderId).orElseThrow(() -> notFound()));
        String old = Objects.toString(o.get("status")),
                oldPayment = Objects.toString(o.get("payment_status"));
        String next = text(a, "status"),
                payment = text(a, "payment_status"),
                note = bounded(a, "note", 0, 300);
        require(
                old.equals(a.get("expected_status"))
                        && oldPayment.equals(a.get("expected_payment_status")),
                HttpStatus.CONFLICT,
                "Đơn vừa thay đổi. Vui lòng tải lại.");
        require(
                next.equals(old) || TRANSITIONS.getOrDefault(old, Set.of()).contains(next),
                HttpStatus.CONFLICT,
                "Chuyển trạng thái không hợp lệ.");
        require(
                !next.equals(old) || !payment.equals(oldPayment),
                HttpStatus.BAD_REQUEST,
                "Chưa có thay đổi để lưu.");
        boolean terminal = Set.of("cancelled", "returned").contains(next);
        if (terminal)
            require(!note.isBlank(), HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do hủy/trả hàng.");
        if (!payment.equals(oldPayment)) {
            boolean allowed =
                    ("unpaid".equals(oldPayment) && "paid".equals(payment) && !terminal)
                            || (Set.of("paid", "partially_refunded").contains(oldPayment)
                                    && "refunded".equals(payment)
                                    && terminal);
            require(allowed, HttpStatus.CONFLICT, "Chuyển trạng thái thanh toán không hợp lệ.");
        }
        require(
                orderRepository.updateExpectedStatus(next, payment, now(), orderId, old, oldPayment)
                        == 1,
                HttpStatus.CONFLICT,
                "Đơn vừa thay đổi. Vui lòng tải lại.");
        if (terminal && !next.equals(old)) {
            require(
                    shipmentRepository.countActiveShipments(orderId) == 0,
                    HttpStatus.CONFLICT,
                    "Vui lòng xử lý vận đơn đang hoạt động trước.");
            for (var line : snapshots(orderItemRepository.findRestockItems(orderId))) {
                int quantity = number(line.get("quantity"));
                productVariantRepository.restoreStock(quantity, id(line.get("variant_id")));
                movement(id(line.get("variant_id")), quantity, "return_in", orderId, actor, note);
            }
        }
        if (!payment.equals(oldPayment)) {
            if ("paid".equals(payment))
                paymentRepository.markLatestPendingPaid(now(), now(), orderId);
            else paymentRepository.markPaidRefunded(now(), orderId);
        }
        if (!next.equals(old) && "confirmed".equals(next))
            orderRepository.recordConfirmation(actor, now(), orderId);
        if (!next.equals(old) && "cancelled".equals(next))
            orderRepository.recordCancellation(actor, now(), note, orderId);
        if (terminal) paymentRepository.cancelPending(now(), now(), orderId);
        history(orderId, old, next, actor, note);
    }

    private void startPayment(Map<String, Object> a, AuthService.User user) {
        long orderId = id(a.get("order_id"));
        locks.lockOrder(orderId);
        var order =
                toRow(
                        orderRepository
                                .findOwnedOrder(orderId, user.id())
                                .orElseThrow(() -> notFound()));
        require(
                "unpaid".equals(order.get("payment_status"))
                        && !Set.of("cancelled", "returned").contains(order.get("status")),
                HttpStatus.CONFLICT,
                "Đơn hàng không thể thanh toán.");
        require(
                paymentRepository.countCodPayments(orderId) == 0,
                HttpStatus.CONFLICT,
                "Đơn này thanh toán khi nhận hàng.");
        require(
                Set.of("mbbank", "payos").contains(text(a, "gateway")),
                HttpStatus.BAD_REQUEST,
                "Hiện hỗ trợ thanh toán QR qua payOS.");
        var request =
                CreatePaymentRequest.builder()
                        .orderId(orderId)
                        .returnUrl(
                                frontendUrl.replaceAll("/+$", "")
                                        + "/payments/result?order="
                                        + orderId)
                        .cancelUrl(frontendUrl.replaceAll("/+$", "") + "/payments?order=" + orderId)
                        .build();
        var payment = payments.createPayment(request);
        paymentRepository.updateCheckoutUrl(payment.getCheckoutUrl(), payment.getPaymentId());
    }

    private void cancelOrder(Map<String, Object> a, AuthService.User user) {
        long orderId = id(a.get("order_id"));
        String reason = bounded(a, "reason", 1, 300);
        // Serialize against shipment creation, management updates and repeat cancellations.
        locks.lockOrder(orderId);
        var order =
                toRow(
                        orderRepository
                                .findOwnedOrder(orderId, user.id())
                                .orElseThrow(() -> notFound()));
        String status = Objects.toString(order.get("status"));
        if ("cancelled".equals(status)) return;
        require(
                Set.of("pending", "confirmed", "preparing").contains(status),
                HttpStatus.CONFLICT,
                "Đơn đã chuyển sang giao hàng hoặc hoàn tất, không thể tự hủy.");
        String paymentStatus = Objects.toString(order.get("payment_status"));
        require(
                Set.of("unpaid", "paid").contains(paymentStatus),
                HttpStatus.CONFLICT,
                "Đơn đang được xử lý hoàn tiền, không thể hủy.");
        require(
                shipmentRepository.countEffectiveShipments(orderId) == 0,
                HttpStatus.CONFLICT,
                "Đơn đã có vận đơn còn hiệu lực. Vui lòng liên hệ hỗ trợ để xử lý.");
        require(
                supportTicketRepository.countCancellationBlockers(orderId) == 0,
                HttpStatus.CONFLICT,
                "Đơn đã có yêu cầu hỗ trợ hoặc hoàn tiền. Vui lòng theo dõi phiếu hỗ trợ hiện tại.");
        payments.closePendingPaymentLinks(orderId, reason);
        // Paid orders retain 'paid' until staff records an actual refund; no money is refunded
        // here.
        updateOrder(
                Map.of(
                        "id",
                        orderId,
                        "expected_status",
                        status,
                        "expected_payment_status",
                        paymentStatus,
                        "status",
                        "cancelled",
                        "payment_status",
                        paymentStatus,
                        "note",
                        reason),
                user.id());
    }

    private void cancelPayment(Map<String, Object> a, AuthService.User user) {
        long orderId = id(a.get("order_id"));
        locks.lockOrder(orderId);
        var order =
                toRow(
                        orderRepository
                                .findOwnedOrder(orderId, user.id())
                                .orElseThrow(() -> notFound()));
        require(
                !"paid".equals(order.get("payment_status")),
                HttpStatus.CONFLICT,
                "Đơn hàng đã thanh toán thành công.");
        String reason = bounded(a, "reason", 0, 300);
        if (reason.isBlank()) reason = "Khách hàng chủ động hủy thanh toán";
        payments.cancelPaymentByOrderId(orderId, reason);
    }

    private Map<String, Object> variant(long variantId) {
        var result =
                toRow(
                        productVariantRepository
                                .findAvailableById(variantId)
                                .orElseThrow(() -> notFound()));
        var product =
                toRow(
                        productRepository
                                .findById(id(result.get("product_id")))
                                .orElseThrow(() -> notFound()));
        result.put("name", product.get("name"));
        result.put("brand_id", product.get("brand_id"));
        result.put("category_id", product.get("category_id"));
        return result;
    }

    private void movement(
            long variantId, int quantity, String type, long orderId, long actor, String note) {
        inventoryMovementRepository.saveAndFlush(
                InventoryMovement.builder()
                        .variant(productVariantRepository.getReferenceById(variantId))
                        .movementType(type)
                        .quantity(quantity)
                        .balanceAfter(
                                number(
                                        toRow(
                                                        productVariantRepository
                                                                .findById(variantId)
                                                                .orElseThrow(() -> notFound()))
                                                .get("stock_quantity")))
                        .refType("order")
                        .refId(orderId)
                        .note(note)
                        .createdBy(actor)
                        .createdAt(now())
                        .build());
    }

    private void history(long orderId, String from, String to, long actor, String note) {
        orderStatusHistoryRepository.saveAndFlush(
                OrderStatusHistory.builder()
                        .orderId(orderId)
                        .fromStatus(from)
                        .toStatus(to)
                        .changedBy(actor)
                        .note(note)
                        .createdAt(now())
                        .build());
    }

    @Override
    @Transactional
    public void checkout(CheckoutRequest request, AuthService.User user) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (request.getSource() != null) action.put("source", request.getSource());
        if (request.getVariantId() != null) action.put("variant_id", request.getVariantId());
        if (request.getQuantity() != null) action.put("quantity", request.getQuantity());
        if (request.getCartItemIds() != null) action.put("cart_item_ids", request.getCartItemIds());
        if (request.getReceiverName() != null)
            action.put("receiver_name", request.getReceiverName());
        if (request.getReceiverPhone() != null)
            action.put("receiver_phone", request.getReceiverPhone());
        if (request.getShippingAddress() != null)
            action.put("shipping_address", request.getShippingAddress());
        if (request.getNote() != null) action.put("note", request.getNote());
        if (request.getPaymentMethod() != null)
            action.put("payment_method", request.getPaymentMethod());
        if (request.getVoucherCode() != null) action.put("voucher_code", request.getVoucherCode());
        action.put("type", "checkout");
        execute(action, user);
    }

    @Override
    @Transactional
    public void cancelOrder(long id, CancelOrderRequest request, AuthService.User user) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (request.getReason() != null) action.put("reason", request.getReason());
        action.put("type", "cancel_order");
        action.put("id", id);
        action.put("order_id", id);
        execute(action, user);
    }

    @Override
    @Transactional
    public void updateStatus(long id, UpdateOrderStatusRequest request, AuthService.User user) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (request.getExpectedStatus() != null)
            action.put("expected_status", request.getExpectedStatus());
        if (request.getExpectedPaymentStatus() != null)
            action.put("expected_payment_status", request.getExpectedPaymentStatus());
        if (request.getStatus() != null) action.put("status", request.getStatus());
        if (request.getPaymentStatus() != null)
            action.put("payment_status", request.getPaymentStatus());
        if (request.getNote() != null) action.put("note", request.getNote());
        action.put("type", "manager_update_order");
        action.put("id", id);
        action.put("order_id", id);
        execute(action, user);
    }

    @Override
    @Transactional
    public void startPayment(long id, StartOrderPaymentRequest request, AuthService.User user) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (request.getGateway() != null) action.put("gateway", request.getGateway());
        action.put("type", "start_payment");
        action.put("id", id);
        action.put("order_id", id);
        execute(action, user);
    }

    @Override
    @Transactional
    public void cancelPayment(long id, CancelOrderRequest request, AuthService.User user) {
        Map<String, Object> action = new LinkedHashMap<>();
        if (request.getReason() != null) action.put("reason", request.getReason());
        action.put("type", "cancel_payment");
        action.put("id", id);
        action.put("order_id", id);
        execute(action, user);
    }

    private static void require(boolean condition, HttpStatus status, String message) {
        if (!condition) throw new OrderException(status, message);
    }

    private static OrderException notFound() {
        return new OrderException(
                HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu hoặc bạn không có quyền truy cập.");
    }

    private static long id(Object value) {
        try {
            return CommerceValidation.id(value);
        } catch (fu.se.badmintonshop.modules.common.exception.BusinessException e) {
            throw new OrderException(e.getStatusCode().value(), e.getReason());
        }
    }

    private static int positiveInt(Object value) {
        try {
            return CommerceValidation.positiveInt(value);
        } catch (fu.se.badmintonshop.modules.common.exception.BusinessException e) {
            throw new OrderException(e.getStatusCode().value(), e.getReason());
        }
    }

    private static String bounded(Map<String, Object> action, String key, int min, int max) {
        try {
            return CommerceValidation.bounded(action, key, min, max);
        } catch (fu.se.badmintonshop.modules.common.exception.BusinessException e) {
            throw new OrderException(e.getStatusCode().value(), e.getReason());
        }
    }
}
