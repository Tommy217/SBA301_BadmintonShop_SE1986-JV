package fu.se.badmintonshop.modules.delivery.service;

import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.invoice.dto.request.InvoiceRequest;
import fu.se.badmintonshop.modules.invoice.repository.InvoiceRepository;
import fu.se.badmintonshop.modules.invoice.service.InvoiceService;
import fu.se.badmintonshop.modules.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

// backend delivery tu chuyen orders.status theo trang thai van don (goi trong transaction cua ShipmentService)
@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {

    private static final Set<String> SHIPPABLE = Set.of("confirmed", "preparing");

    private final JdbcTemplate jdbc;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    // tra ve trang thai don moi, hoac null neu khong doi
    public String onShipmentStatus(Order order, ShipmentStatus status, Long actorId) {
        String from = order.getStatus();
        String to = switch (status) {
            case PENDING_PICKUP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERY_FAILED ->
                    SHIPPABLE.contains(from) ? "shipping" : null;
            case DELIVERED -> Set.of("completed", "cancelled", "returned").contains(from) ? null : "completed";
            case RETURNED -> Set.of("returned", "cancelled").contains(from) ? null : "returned";
            // van don bi huy / tu choi khi don dang giao: tra don ve buoc chuan bi de tao van don moi
            case CANCELLED, REJECTED -> "shipping".equals(from) ? "preparing" : null;
            case REQUESTED -> null;
        };
        if (to == null || to.equals(from)) {
            return null;
        }

        order.setStatus(to);
        jdbc.update("INSERT INTO order_status_history(order_id,from_status,to_status,changed_by,note,created_at) VALUES(?,?,?,?,?,?)",
                order.getId(), from, to, actorId, "Cập nhật theo vận đơn: " + status.getLabel(), LocalDateTime.now());

        if ("returned".equals(to)) {
            restock(order.getId());
        }
        if ("completed".equals(to) && "paid".equals(order.getPaymentStatus())
                && !invoiceRepository.existsByOrderIdAndInvoiceType(order.getId(), "sale")) {
            InvoiceRequest request = new InvoiceRequest();
            request.setOrderId(order.getId());
            invoiceService.create(request);
        }
        return to;
    }

    // hang chuyen hoan ve shop: cong lai ton kho + ghi so kho
    private void restock(Long orderId) {
        List<Map<String, Object>> lines = jdbc.queryForList(
                "SELECT variant_id,quantity FROM order_items WHERE order_id=? ORDER BY variant_id", orderId);
        for (Map<String, Object> line : lines) {
            long variantId = ((Number) line.get("variant_id")).longValue();
            int quantity = ((Number) line.get("quantity")).intValue();
            jdbc.update("UPDATE product_variants SET stock_quantity=stock_quantity+? WHERE id=?", quantity, variantId);
            jdbc.update("INSERT INTO inventory_movements(variant_id,movement_type,quantity,balance_after,ref_type,ref_id,note,created_at) "
                            + "VALUES(?,'return_in',?,(SELECT stock_quantity FROM product_variants WHERE id=?),'order',?,?,?)",
                    variantId, quantity, variantId, orderId, "Đơn vị vận chuyển chuyển hoàn", LocalDateTime.now());
        }
    }
}
