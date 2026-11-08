package fu.se.badmintonshop.modules.delivery.service;

import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.invoice.dto.request.InvoiceRequest;
import fu.se.badmintonshop.modules.invoice.repository.InvoiceRepository;
import fu.se.badmintonshop.modules.invoice.service.InvoiceService;
import fu.se.badmintonshop.modules.order.entity.Order;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.*;
import fu.se.badmintonshop.modules.order.entity.OrderItem;
import fu.se.badmintonshop.modules.order.entity.OrderStatusHistory;
import fu.se.badmintonshop.modules.catalog.entity.ProductVariant;
import fu.se.badmintonshop.modules.inventory.entity.InventoryMovement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

// backend delivery tu chuyen orders.status theo trang thai van don (goi trong transaction cua
// ShipmentService)
@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {

    private static final Set<String> SHIPPABLE = Set.of("confirmed", "preparing");

    private final EntityManager em;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    // tra ve trang thai don moi, hoac null neu khong doi
    public String onShipmentStatus(Order order, ShipmentStatus status, Long actorId) {
        String from = order.getStatus();
        String to =
                switch (status) {
                    case PENDING_PICKUP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERY_FAILED ->
                            SHIPPABLE.contains(from) ? "shipping" : null;
                    case DELIVERED ->
                            Set.of("completed", "cancelled", "returned").contains(from)
                                    ? null
                                    : "completed";
                    case RETURNED ->
                            Set.of("returned", "cancelled").contains(from) ? null : "returned";
                    // van don bi huy / tu choi khi don dang giao: tra don ve buoc chuan bi de tao
                    // van don moi
                    case CANCELLED, REJECTED -> "shipping".equals(from) ? "preparing" : null;
                    case REQUESTED -> null;
                };
        if (to == null || to.equals(from)) {
            return null;
        }

        order.setStatus(to);
        em.persist(
                OrderStatusHistory.builder()
                        .orderId(order.getId())
                        .fromStatus(from)
                        .toStatus(to)
                        .changedBy(actorId)
                        .note("Cập nhật theo vận đơn: " + status.getLabel())
                        .createdAt(LocalDateTime.now())
                        .build());

        if ("returned".equals(to)) {
            restock(order.getId());
        }
        if ("completed".equals(to)
                && "paid".equals(order.getPaymentStatus())
                && !invoiceRepository.existsByOrderIdAndInvoiceType(order.getId(), "sale")) {
            InvoiceRequest request = new InvoiceRequest();
            request.setOrderId(order.getId());
            invoiceService.create(request);
        }
        return to;
    }

    // hang chuyen hoan ve shop: cong lai ton kho + ghi so kho
    private void restock(Long orderId) {
        var lines =
                em.createQuery(
                                "SELECT i FROM OrderItem i WHERE i.orderId=:id ORDER BY i.variantId",
                                OrderItem.class)
                        .setParameter("id", orderId)
                        .getResultList();
        for (var line : lines) {
            em.flush();
            ProductVariant variant =
                    em.find(
                            ProductVariant.class,
                            line.getVariantId(),
                            LockModeType.PESSIMISTIC_WRITE);
            em.refresh(variant, LockModeType.PESSIMISTIC_WRITE);
            variant.setStockQuantity(variant.getStockQuantity() + line.getQuantity());
            em.persist(
                    InventoryMovement.builder()
                            .variant(variant)
                            .movementType("return_in")
                            .quantity(line.getQuantity())
                            .balanceAfter(variant.getStockQuantity())
                            .refType("order")
                            .refId(orderId)
                            .note("Đơn vị vận chuyển chuyển hoàn")
                            .createdAt(LocalDateTime.now())
                            .build());
        }
    }
}
