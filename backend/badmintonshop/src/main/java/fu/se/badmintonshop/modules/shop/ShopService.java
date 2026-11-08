package fu.se.badmintonshop.modules.shop;

import fu.se.badmintonshop.modules.auth.service.AuthService;
import fu.se.badmintonshop.modules.payment.dto.request.CreatePaymentRequest;
import fu.se.badmintonshop.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** SQL-backed snapshot facade used by the customer and management React pages. */
@Service
@RequiredArgsConstructor
public class ShopService {
    private final JdbcTemplate jdbc;
    private final SupportActions support;
    private final PaymentService payments;
    @Value("${shop.frontend-url:http://localhost:5173}") private String frontendUrl;
    private static final Set<String> CATALOG_IDS = Set.of("variant_id", "product_id", "voucher_id");
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "pending", Set.of("confirmed", "cancelled"), "confirmed", Set.of("preparing", "cancelled"),
            "preparing", Set.of("cancelled"), "shipping", Set.of(), "completed", Set.of("returned"),
            "cancelled", Set.of(), "returned", Set.of());

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
        for (String name : List.of("carts", "cart_items", "orders", "order_items", "payments", "order_status_history",
                "support_tickets", "ticket_items", "ticket_status_history", "ticket_inspections", "refund_records")) {
            result.put(name, new ArrayList<>());
        }
        if (user == null) return result;

        String orderWhere = staff ? "" : " WHERE o.user_id=?";
        Object[] scope = staff ? new Object[]{} : new Object[]{user.id()};
        result.put("carts", rows("SELECT * FROM carts WHERE user_id=?", user.id()));
        result.put("cart_items", rows("SELECT i.* FROM cart_items i JOIN carts c ON c.id=i.cart_id WHERE c.user_id=? ORDER BY i.id", user.id()));
        result.put("orders", rows("SELECT o.* FROM orders o" + orderWhere + " ORDER BY o.id", scope));
        result.put("order_items", rows("SELECT i.* FROM order_items i JOIN orders o ON o.id=i.order_id" + orderWhere + " ORDER BY i.id", scope));
        result.put("payments", rows("SELECT p.* FROM payments p JOIN orders o ON o.id=p.order_id" + orderWhere + " ORDER BY p.id", scope));
        result.put("order_status_history", rows("SELECT h.* FROM order_status_history h JOIN orders o ON o.id=h.order_id" + orderWhere + " ORDER BY h.id", scope));
        var tickets = rows("SELECT t.* FROM support_tickets t JOIN orders o ON o.id=t.order_id" + orderWhere + " ORDER BY t.id", scope);
        for (var ticket : tickets) {
            ticket.put("evidence_urls", rows("SELECT file_url FROM ticket_evidence WHERE ticket_id=? ORDER BY id", id(ticket.get("id")))
                    .stream().map(e -> e.get("file_url")).toList());
            ticket.put("inspection", rows("SELECT * FROM ticket_inspection WHERE ticket_id=?", id(ticket.get("id"))).stream().findFirst().orElse(null));
            ticket.put("refund_record", rows("SELECT * FROM refund_records WHERE ticket_id=?", id(ticket.get("id"))).stream().findFirst().orElse(null));
        }
        result.put("support_tickets", tickets);
        for (var entry : Map.of("ticket_items", "ticket_items", "ticket_status_history", "ticket_status_history",
                "ticket_inspections", "ticket_inspection", "refund_records", "refund_records").entrySet()) {
            result.put(entry.getKey(), rows("SELECT x.* FROM " + entry.getValue() + " x JOIN support_tickets t ON t.id=x.ticket_id JOIN orders o ON o.id=t.order_id" + orderWhere + " ORDER BY x.id", scope));
        }
        // van don doc qua API cua module delivery (/api/manager/shipments, /api/my/shipments)
        result.put("refund_approval_threshold", support.getTickets().getApprovalThreshold());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> voucherWallet(AuthService.User user) {
        require(user != null, HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        return Map.of(
                "vouchers", rows("SELECT v.* FROM vouchers v WHERE v.audience='public' OR EXISTS (SELECT 1 FROM user_vouchers uv WHERE uv.voucher_id=v.id AND uv.user_id=?) ORDER BY v.ends_at,v.id", user.id()),
                "voucher_usages", rows("SELECT * FROM voucher_usages WHERE user_id=?", user.id()));
    }

    @Transactional
    public void execute(Map<String, Object> action, AuthService.User user) {
        require(user != null, HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        String type = text(action, "type");
        switch (type) {
            case "add", "update", "remove", "select_all" -> cart(action, user.id());
            case "checkout" -> checkout(action, user.id());
            case "manager_update_order" -> {
                require(management(user), HttpStatus.FORBIDDEN, "Chỉ nhân viên hoặc quản lý được xử lý đơn.");
                updateOrder(action, user.id());
            }
            case "start_payment" -> startPayment(action, user);
            default -> support.execute(type, action, user);
        }
    }

    private void cart(Map<String, Object> action, long userId) {
        // Updating the user row serializes cart creation and writes for this owner.
        jdbc.update("UPDATE users SET updated_at=updated_at WHERE id=?", userId);
        var carts = rows("SELECT id FROM carts WHERE user_id=?", userId);
        long cartId = carts.isEmpty() ? insert("INSERT INTO carts(user_id,updated_at) VALUES(?,?)", userId, now()) : id(carts.getFirst().get("id"));
        switch (text(action, "type")) {
            case "add" -> {
                long variantId = id(action.get("variant_id"));
                var variant = variant(variantId);
                var existing = rows("SELECT id,quantity FROM cart_items WHERE cart_id=? AND variant_id=?", cartId, variantId);
                int addedQuantity = positiveInt(action.getOrDefault("quantity", 1));
                long totalQuantity = (existing.isEmpty() ? 0L : number(existing.getFirst().get("quantity"))) + addedQuantity;
                require(totalQuantity <= number(variant.get("stock_quantity")), HttpStatus.CONFLICT, "Sản phẩm không đủ tồn kho.");
                int quantity = (int) totalQuantity;
                require(quantity <= number(variant.get("stock_quantity")), HttpStatus.CONFLICT, "Sản phẩm không đủ tồn kho.");
                if (existing.isEmpty()) insert("INSERT INTO cart_items(cart_id,variant_id,quantity,is_selected,added_at) VALUES(?,?,?,?,?)", cartId, variantId, quantity, true, now());
                else jdbc.update("UPDATE cart_items SET quantity=? WHERE id=?", quantity, id(existing.getFirst().get("id")));
            }
            case "select_all" -> {
                require(action.get("selected") instanceof Boolean, HttpStatus.BAD_REQUEST, "Lựa chọn không hợp lệ.");
                jdbc.update("UPDATE cart_items SET is_selected=? WHERE cart_id=?", action.get("selected"), cartId);
            }
            default -> {
                long itemId = id(action.get("id"));
                var item = one("SELECT * FROM cart_items WHERE id=? AND cart_id=?", itemId, cartId);
                if ("remove".equals(action.get("type"))) jdbc.update("DELETE FROM cart_items WHERE id=? AND cart_id=?", itemId, cartId);
                else {
                    if (action.containsKey("quantity")) {
                        int quantity = positiveInt(action.get("quantity"));
                        require(quantity <= number(variant(id(item.get("variant_id"))).get("stock_quantity")), HttpStatus.CONFLICT, "Số lượng vượt tồn kho.");
                        jdbc.update("UPDATE cart_items SET quantity=? WHERE id=?", quantity, itemId);
                    }
                    if (action.containsKey("is_selected")) {
                        require(action.get("is_selected") instanceof Boolean, HttpStatus.BAD_REQUEST, "Lựa chọn không hợp lệ.");
                        jdbc.update("UPDATE cart_items SET is_selected=? WHERE id=?", action.get("is_selected"), itemId);
                    }
                }
            }
        }
        jdbc.update("UPDATE carts SET updated_at=? WHERE id=?", now(), cartId);
    }

    private void checkout(Map<String, Object> a, long userId) {
        String name = bounded(a, "receiver_name", 1, 150), phone = bounded(a, "receiver_phone", 1, 20);
        require(phone.matches("(0|\\+84)[0-9]{9}"), HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ.");
        String address = bounded(a, "shipping_address", 10, 500), note = bounded(a, "note", 0, 500);
        String method = text(a, "payment_method");
        require(Set.of("cod", "prepaid").contains(method), HttpStatus.BAD_REQUEST, "Phương thức thanh toán không hợp lệ.");
        jdbc.update("UPDATE users SET updated_at=updated_at WHERE id=?", userId);
        List<Map<String, Object>> lines;
        if ("direct".equals(a.get("source"))) {
            lines = List.of(new LinkedHashMap<>(Map.of("variant_id", id(a.get("variant_id")), "quantity", positiveInt(a.get("quantity")))));
        } else {
            require("cart".equals(a.get("source")), HttpStatus.BAD_REQUEST, "Nguồn đặt hàng không hợp lệ.");
            lines = rows("SELECT i.* FROM cart_items i JOIN carts c ON c.id=i.cart_id WHERE c.user_id=? ORDER BY i.variant_id", userId);
            if (a.containsKey("cart_item_ids")) {
                require(a.get("cart_item_ids") instanceof List<?>, HttpStatus.BAD_REQUEST, "Danh sách giỏ hàng không hợp lệ.");
                var requested = ((List<?>) a.get("cart_item_ids")).stream().map(ShopService::id).toList();
                require(!requested.isEmpty() && new HashSet<>(requested).size() == requested.size(), HttpStatus.BAD_REQUEST, "Chọn ít nhất một sản phẩm, không trùng lặp.");
                lines = lines.stream().filter(i -> requested.contains(id(i.get("id")))).toList();
                require(lines.size() == requested.size(), HttpStatus.CONFLICT, "Sản phẩm không còn trong giỏ của bạn.");
            } else lines = lines.stream().filter(i -> Boolean.TRUE.equals(i.get("is_selected"))).toList();
        }
        require(!lines.isEmpty(), HttpStatus.BAD_REQUEST, "Chọn ít nhất một sản phẩm.");
        BigDecimal subtotal = BigDecimal.ZERO;
        // All orders acquire SKU locks in the same order; atomic deductions prevent overselling.
        lines = new ArrayList<>(lines);
        lines.sort(Comparator.comparingLong(i -> id(i.get("variant_id"))));
        for (var line : lines) {
            int quantity = positiveInt(line.get("quantity"));
            long variantId = id(line.get("variant_id"));
            require(jdbc.update("UPDATE product_variants SET stock_quantity=stock_quantity-? WHERE id=? AND is_active=1 AND stock_quantity>=?", quantity, variantId, quantity) == 1,
                    HttpStatus.CONFLICT, "Sản phẩm không đủ tồn kho hoặc đã ngừng bán.");
            var v = variant(variantId);
            line.put("variant", v);
            subtotal = subtotal.add(money(v.get("price")).multiply(BigDecimal.valueOf(quantity)));
        }
        var voucher = applyVoucher(text(a, "voucher_code"), userId, lines, subtotal);
        BigDecimal discount = money(voucher.getOrDefault("discount", BigDecimal.ZERO));
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount).add(fee);
        require(total.compareTo(new BigDecimal("999999999999")) <= 0, HttpStatus.BAD_REQUEST, "Giá trị đơn hàng vượt giới hạn.");
        long orderId = insert("INSERT INTO orders(order_code,user_id,channel,status,payment_status,receiver_name,receiver_phone,shipping_address,voucher_id,voucher_code,subtotal,discount_amount,shipping_fee,total_amount,note,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "BS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24), userId, "online", "pending", "unpaid", name, phone, address,
                voucher.get("id"), voucher.get("code"), subtotal, discount, fee, total, note, now(), now());
        for (var line : lines) {
            @SuppressWarnings("unchecked") var v = (Map<String, Object>) line.get("variant");
            int quantity = positiveInt(line.get("quantity"));
            insert("INSERT INTO order_items(order_id,variant_id,product_id,product_name,variant_info,unit_price,quantity,line_total) VALUES(?,?,?,?,?,?,?,?)",
                    orderId, id(v.get("id")), id(v.get("product_id")), v.get("name"), Objects.toString(v.get("color"), "") + " / " + Objects.toString(v.get("size"), ""), v.get("price"), quantity, money(v.get("price")).multiply(BigDecimal.valueOf(quantity)));
            movement(id(v.get("id")), -quantity, "stock_out", orderId, userId, "Đặt hàng");
            if (line.containsKey("id")) jdbc.update("DELETE FROM cart_items WHERE id=?", id(line.get("id")));
        }
        insert("INSERT INTO payments(order_id,method,status,amount,created_at,updated_at) VALUES(?,?,?,?,?,?)", orderId, "cod".equals(method) ? "cod" : "qr", "pending", total, now(), now());
        history(orderId, null, "pending", userId, "Khách hàng đặt hàng");
        if (voucher.containsKey("id")) insert("INSERT INTO voucher_usages(voucher_id,user_id,order_id,discount_amount,used_at) VALUES(?,?,?,?,?)", voucher.get("id"), userId, orderId, discount, now());
    }

    private Map<String, Object> applyVoucher(String code, long userId, List<Map<String, Object>> lines, BigDecimal subtotal) {
        if (code.isBlank()) return Map.of();
        // The write locks this voucher until commit, also serializing per-user usage checks.
        require(jdbc.update("UPDATE vouchers SET used_count=used_count+1 WHERE UPPER(code)=? AND is_active=1 AND starts_at<=? AND ends_at>=? AND (usage_limit IS NULL OR used_count<usage_limit)", code.toUpperCase(Locale.ROOT), now(), now()) == 1,
                HttpStatus.CONFLICT, "Voucher không hợp lệ, hết hạn hoặc hết lượt sử dụng.");
        require(jdbc.queryForObject("SELECT COUNT(*) FROM vouchers v WHERE (v.audience='public' OR EXISTS (SELECT 1 FROM user_vouchers uv WHERE uv.voucher_id=v.id AND uv.user_id=?)) AND UPPER(v.code)=?", Long.class, userId, code.toUpperCase(Locale.ROOT)) > 0,
                HttpStatus.FORBIDDEN, "Mã giảm giá chưa được cấp cho tài khoản của bạn.");
        var voucher = one("SELECT * FROM vouchers WHERE UPPER(code)=?", code.toUpperCase(Locale.ROOT));
        long voucherId = id(voucher.get("id"));
        require(jdbc.queryForObject("SELECT COUNT(*) FROM voucher_usages WHERE voucher_id=? AND user_id=?", Long.class, voucherId, userId) < number(voucher.get("per_user_limit")), HttpStatus.CONFLICT, "Bạn đã dùng hết lượt voucher.");
        require(subtotal.compareTo(money(voucher.get("min_order"))) >= 0, HttpStatus.BAD_REQUEST, "Đơn hàng chưa đạt giá trị tối thiểu của voucher.");
        var scopes = rows("SELECT scope_type,scope_id FROM voucher_scopes WHERE voucher_id=?", voucherId);
        BigDecimal eligible = BigDecimal.ZERO;
        String applies = Objects.toString(voucher.get("applies_to"));
        for (var line : lines) {
            @SuppressWarnings("unchecked") var v = (Map<String, Object>) line.get("variant");
            String key = switch (applies) { case "brand" -> "brand_id"; case "category" -> "category_id"; default -> "product_id"; };
            if ("all".equals(applies) || scopes.stream().anyMatch(s -> applies.equals(s.get("scope_type")) && id(s.get("scope_id")) == id(v.get(key)))) {
                eligible = eligible.add(money(v.get("price")).multiply(BigDecimal.valueOf(positiveInt(line.get("quantity")))));
            }
        }
        require(eligible.signum() > 0, HttpStatus.BAD_REQUEST, "Sản phẩm không thuộc phạm vi voucher.");
        BigDecimal discount = money(voucher.get("discount_value"));
        if ("percent".equals(voucher.get("discount_type"))) discount = eligible.multiply(discount).divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
        if (voucher.get("max_discount") != null) discount = discount.min(money(voucher.get("max_discount")));
        voucher.put("discount", discount.min(eligible));
        return voucher;
    }

    private void updateOrder(Map<String, Object> a, long actor) {
        long orderId = id(a.get("id"));
        var o = one("SELECT * FROM orders WHERE id=?", orderId);
        String old = Objects.toString(o.get("status")), oldPayment = Objects.toString(o.get("payment_status"));
        String next = text(a, "status"), payment = text(a, "payment_status"), note = bounded(a, "note", 0, 300);
        require(old.equals(a.get("expected_status")) && oldPayment.equals(a.get("expected_payment_status")), HttpStatus.CONFLICT, "Đơn vừa thay đổi. Vui lòng tải lại.");
        require(next.equals(old) || TRANSITIONS.getOrDefault(old, Set.of()).contains(next), HttpStatus.CONFLICT, "Chuyển trạng thái không hợp lệ.");
        require(!next.equals(old) || !payment.equals(oldPayment), HttpStatus.BAD_REQUEST, "Chưa có thay đổi để lưu.");
        boolean terminal = Set.of("cancelled", "returned").contains(next);
        if (terminal) require(!note.isBlank(), HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do hủy/trả hàng.");
        if (!payment.equals(oldPayment)) {
            boolean allowed = ("unpaid".equals(oldPayment) && "paid".equals(payment) && !terminal)
                    || (Set.of("paid", "partially_refunded").contains(oldPayment) && "refunded".equals(payment) && terminal);
            require(allowed, HttpStatus.CONFLICT, "Chuyển trạng thái thanh toán không hợp lệ.");
        }
        require(jdbc.update("UPDATE orders SET status=?,payment_status=?,updated_at=? WHERE id=? AND status=? AND payment_status=?", next, payment, now(), orderId, old, oldPayment) == 1,
                HttpStatus.CONFLICT, "Đơn vừa thay đổi. Vui lòng tải lại.");
        if (terminal && !next.equals(old)) {
            require(jdbc.queryForObject("SELECT COUNT(*) FROM shipments WHERE order_id=? AND status NOT IN ('delivered','cancelled','returned','rejected')", Long.class, orderId) == 0, HttpStatus.CONFLICT, "Vui lòng xử lý vận đơn đang hoạt động trước.");
            for (var line : rows("SELECT variant_id,quantity FROM order_items WHERE order_id=? ORDER BY variant_id", orderId)) {
                int quantity = number(line.get("quantity"));
                jdbc.update("UPDATE product_variants SET stock_quantity=stock_quantity+? WHERE id=?", quantity, id(line.get("variant_id")));
                movement(id(line.get("variant_id")), quantity, "return_in", orderId, actor, note);
            }
        }
        if (!payment.equals(oldPayment)) {
            if ("paid".equals(payment)) jdbc.update("UPDATE payments SET status='paid',paid_at=?,updated_at=? WHERE id=(SELECT MAX(id) FROM payments WHERE order_id=? AND status='pending')", now(), now(), orderId);
            else jdbc.update("UPDATE payments SET status='refunded',updated_at=? WHERE order_id=? AND status='paid'", now(), orderId);
        }
        if (!next.equals(old) && "confirmed".equals(next)) jdbc.update("UPDATE orders SET confirmed_by=?,confirmed_at=? WHERE id=?", actor, now(), orderId);
        if (!next.equals(old) && "cancelled".equals(next)) jdbc.update("UPDATE orders SET cancelled_by=?,cancelled_at=?,cancel_reason=? WHERE id=?", actor, now(), note, orderId);
        if (terminal) jdbc.update("UPDATE payments SET status='cancelled',cancelled_at=?,updated_at=? WHERE order_id=? AND status='pending'", now(), now(), orderId);
        history(orderId, old, next, actor, note);
    }

    private void startPayment(Map<String, Object> a, AuthService.User user) {
        long orderId = id(a.get("order_id"));
        jdbc.update("UPDATE orders SET updated_at=updated_at WHERE id=? AND user_id=?", orderId, user.id());
        var order = one("SELECT * FROM orders WHERE id=? AND user_id=?", orderId, user.id());
        require("unpaid".equals(order.get("payment_status")) && !Set.of("cancelled", "returned").contains(order.get("status")), HttpStatus.CONFLICT, "Đơn hàng không thể thanh toán.");
        require(jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE order_id=? AND method='cod'", Long.class, orderId) == 0, HttpStatus.CONFLICT, "Đơn này thanh toán khi nhận hàng.");
        require(Set.of("mbbank", "payos").contains(text(a, "gateway")), HttpStatus.BAD_REQUEST, "Hiện hỗ trợ thanh toán QR qua payOS.");
        var request = CreatePaymentRequest.builder().orderId(orderId)
                .returnUrl(frontendUrl.replaceAll("/+$", "") + "/payments/result?order=" + orderId)
                .cancelUrl(frontendUrl.replaceAll("/+$", "") + "/payments?order=" + orderId).build();
        var payment = payments.createPayment(request);
        jdbc.update("UPDATE payments SET checkout_url=? WHERE id=?", payment.getCheckoutUrl(), payment.getPaymentId());
    }

    private Map<String, Object> variant(long variantId) {
        return one("SELECT v.*,p.name,p.brand_id,p.category_id FROM product_variants v JOIN products p ON p.id=v.product_id JOIN brands b ON b.id=p.brand_id JOIN categories c ON c.id=p.category_id WHERE v.id=? AND v.is_active=1 AND p.is_active=1 AND b.is_active=1 AND c.is_active=1", variantId);
    }

    private void movement(long variantId, int quantity, String type, long orderId, long actor, String note) {
        insert("INSERT INTO inventory_movements(variant_id,movement_type,quantity,balance_after,ref_type,ref_id,note,created_by,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
                variantId, type, quantity, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=?", Integer.class, variantId), "order", orderId, note, actor, now());
    }

    private void history(long orderId, String from, String to, long actor, String note) {
        insert("INSERT INTO order_status_history(order_id,from_status,to_status,changed_by,note,created_at) VALUES(?,?,?,?,?,?)", orderId, from, to, actor, note, now());
    }

    List<Map<String, Object>> rows(String sql, Object... parameters) {
        return jdbc.query(sql, (rs, index) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                String key = rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT);
                Object value = rs.getObject(i);
                if (value instanceof Timestamp timestamp) value = timestamp.toLocalDateTime().toString();
                if (value instanceof Clob clob) value = clob.getSubString(1, Math.toIntExact(clob.length()));
                if (value instanceof Number && (key.equals("id") || key.endsWith("_id")) && !CATALOG_IDS.contains(key)) value = value.toString();
                row.put(key, value);
            }
            return row;
        }, parameters);
    }

    private Map<String, Object> one(String sql, Object... args) {
        var result = rows(sql, args);
        require(!result.isEmpty(), HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu hoặc bạn không có quyền truy cập.");
        return result.getFirst();
    }

    private long insert(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            return statement;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    public static boolean management(AuthService.User user) { return user != null && Set.of("STAFF", "MANAGER", "ADMIN").contains(user.role()); }
    public static long id(Object value) {
        try { long id = Long.parseLong(Objects.toString(value, "")); if (id > 0) return id; } catch (NumberFormatException ignored) { }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã dữ liệu không hợp lệ.");
    }
    private static int positiveInt(Object value) {
        try { int quantity = new BigDecimal(Objects.toString(value, "")).intValueExact(); if (quantity > 0) return quantity; } catch (ArithmeticException | NumberFormatException ignored) { }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phải là số nguyên dương.");
    }
    private static int number(Object value) { return ((Number) value).intValue(); }
    private static BigDecimal money(Object value) { return new BigDecimal(Objects.toString(value, "0")); }
    private static Timestamp now() { return Timestamp.valueOf(LocalDateTime.now()); }
    static String text(Map<String, Object> a, String key) { return Objects.toString(a.get(key), "").trim(); }
    private static String bounded(Map<String, Object> a, String key, int min, int max) {
        String value = text(a, key);
        require(value.length() >= min && value.length() <= max, HttpStatus.BAD_REQUEST, "Trường " + key + " cần từ " + min + " đến " + max + " ký tự.");
        return value;
    }
    static void require(boolean condition, HttpStatus status, String message) { if (!condition) throw new ResponseStatusException(status, message); }
}
