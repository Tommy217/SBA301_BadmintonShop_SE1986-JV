package fu.se.badmintonshop.modules.shop;

import fu.se.badmintonshop.modules.auth.AuthService;
import fu.se.badmintonshop.modules.delivery.dto.CreateShipmentRequest;
import fu.se.badmintonshop.modules.delivery.entity.ShipmentEvent;
import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.delivery.gateway.FastShipCarrierGateway;
import fu.se.badmintonshop.modules.delivery.repository.*;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import fu.se.badmintonshop.modules.invoice.dto.request.InvoiceRequest;
import fu.se.badmintonshop.modules.invoice.repository.InvoiceRepository;
import fu.se.badmintonshop.modules.invoice.service.InvoiceService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static fu.se.badmintonshop.modules.shop.ShopService.*;

@Service @RequiredArgsConstructor
class ShopShipping {
    private final ShipmentRepository shipments;
    private final ShipmentEventRepository events;
    private final FastShipCarrierGateway gateway;
    private final ShipmentService shipmentService;
    private final InvoiceService invoices;
    private final InvoiceRepository invoiceRepository;
    private final EntityManager entities;
    private final JdbcTemplate jdbc;

    @Transactional
    public void create(long orderId, long staffId) {
        require(entities.find(fu.se.badmintonshop.modules.order.entity.Order.class, orderId, LockModeType.PESSIMISTIC_WRITE) != null,
                HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.");
        if (shipments.existsByOrderIdAndStatusNotIn(orderId, List.of("cancelled", "rejected"))) return;
        shipmentService.create(CreateShipmentRequest.builder().orderId(orderId).carrierCode("fastship").serviceType("standard").weightGrams(500).build(), staffId);
    }

    @Transactional
    public void sync(long shipmentId, AuthService.User user) {
        var shipment = shipments.findById(shipmentId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vận đơn."));
        require(management(user) || Objects.equals(shipment.getOrder().getUserId(), user.id()), HttpStatus.NOT_FOUND, "Không tìm thấy vận đơn.");
        var remote = gateway.track(shipment.getTrackingCode());
        entities.lock(shipment, LockModeType.PESSIMISTIC_WRITE);
        entities.refresh(shipment);
        String status = remote.path("status").asText();
        require(ShipmentStatus.fromValue(status).isPresent(), HttpStatus.BAD_GATEWAY, "Đối tác trả trạng thái vận đơn không hợp lệ.");
        require(Objects.equals(shipment.getTrackingCode(), remote.path("trackingCode").asText()), HttpStatus.BAD_GATEWAY, "Mã vận đơn từ đối tác không khớp.");
        // Read-only synchronization: never advance the partner's status from the shop UI.
        String old = shipment.getStatus();
        if (ShipmentStatus.fromValue(old).orElseThrow().isFinal() && !old.equals(status)) return;
        shipment.setStatus(status);
        shipment.setCarrierStatusText(remote.path("statusText").asText(null));
        shipment.setShipperName(remote.path("shipperName").asText(null));
        shipment.setShipperPhone(remote.path("shipperPhone").asText(null));
        shipment.setLastSyncedAt(LocalDateTime.now());
        if (remote.hasNonNull("deliveredAt")) shipment.setDeliveredAt(LocalDateTime.parse(remote.get("deliveredAt").asText()));
        if (remote.hasNonNull("shippedAt")) shipment.setShippedAt(LocalDateTime.parse(remote.get("shippedAt").asText()));
        var known = new HashSet<String>();
        events.findByShipmentIdOrderByOccurredAtDesc(shipmentId).forEach(e -> known.add(e.getExternalEventId()));
        for (var event : remote.path("events")) {
            String externalId = "FS-" + shipmentId + "-" + event.path("id").asText();
            if (!known.add(externalId)) continue;
            shipment.addEvent(ShipmentEvent.builder().externalEventId(externalId).status(event.path("status").asText())
                    .title(event.path("title").asText()).description(shortText(event.path("description").asText(), 300))
                    .location(shortText(event.path("location").asText(), 200)).source("sync")
                    .occurredAt(LocalDateTime.parse(event.path("occurredAt").asText())).build());
        }
        var order = shipment.getOrder();
        entities.lock(order, LockModeType.PESSIMISTIC_WRITE);
        String from = order.getStatus();
        if ("delivered".equals(status) && !Set.of("completed", "cancelled", "returned").contains(from)) {
            order.setStatus("completed");
            if (shipment.getCodAmount().signum() > 0 && "unpaid".equals(order.getPaymentStatus())) {
                order.setPaymentStatus("paid");
                shipment.setCodCollectedAt(LocalDateTime.now());
                jdbc.update("UPDATE payments SET status='paid',paid_at=?,updated_at=? WHERE order_id=? AND method='cod' AND status='pending'", LocalDateTime.now(), LocalDateTime.now(), order.getId());
            }
        } else if (Set.of("in_transit", "out_for_delivery", "delivery_failed").contains(status) && Set.of("confirmed", "preparing").contains(from)) {
            order.setStatus("shipping");
        } else if ("returned".equals(status) && !Set.of("returned", "cancelled").contains(from)) {
            order.setStatus("returned");
            var lines = jdbc.queryForList("SELECT variant_id,quantity FROM order_items WHERE order_id=? ORDER BY variant_id", order.getId());
            for (var line : lines) {
                long variantId = ((Number) line.get("variant_id")).longValue();
                int quantity = ((Number) line.get("quantity")).intValue();
                jdbc.update("UPDATE product_variants SET stock_quantity=stock_quantity+? WHERE id=?", quantity, variantId);
                jdbc.update("INSERT INTO inventory_movements(variant_id,movement_type,quantity,balance_after,ref_type,ref_id,note,created_at) VALUES(?,'return_in',?,(SELECT stock_quantity FROM product_variants WHERE id=?),'order',?,'FastShip chuyển hoàn',?)", variantId, quantity, variantId, order.getId(), LocalDateTime.now());
            }
        }
        if (!from.equals(order.getStatus())) jdbc.update("INSERT INTO order_status_history(order_id,from_status,to_status,changed_by,note,created_at) VALUES(?,?,?,?,?,?)", order.getId(), from, order.getStatus(), user.id(), "Đồng bộ FastShip", LocalDateTime.now());
        shipments.saveAndFlush(shipment);
        if ("completed".equals(order.getStatus()) && "paid".equals(order.getPaymentStatus()) && !invoiceRepository.existsByOrderIdAndInvoiceType(order.getId(), "sale")) {
            var request = new InvoiceRequest(); request.setOrderId(order.getId()); invoices.create(request);
        }
    }
    private String shortText(String text, int length) { return text.substring(0, Math.min(text.length(), length)); }
}
