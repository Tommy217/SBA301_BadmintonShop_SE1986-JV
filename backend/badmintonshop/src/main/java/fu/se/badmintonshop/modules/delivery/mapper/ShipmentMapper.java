package fu.se.badmintonshop.modules.delivery.mapper;

import fu.se.badmintonshop.modules.delivery.dto.CarrierResponse;
import fu.se.badmintonshop.modules.delivery.dto.ShipmentEventResponse;
import fu.se.badmintonshop.modules.delivery.dto.ShipmentResponse;
import fu.se.badmintonshop.modules.delivery.entity.Carrier;
import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import fu.se.badmintonshop.modules.delivery.entity.ShipmentEvent;
import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.order.entity.Order;

import java.util.Optional;

public final class ShipmentMapper {

    private ShipmentMapper() {
    }

    // ban cho manager/staff; withEvents = true khi xem chi tiet
    public static ShipmentResponse toResponse(Shipment s, boolean withEvents) {
        Order order = s.getOrder();
        Carrier carrier = s.getCarrier();
        Optional<ShipmentStatus> status = ShipmentStatus.fromValue(s.getStatus());

        return ShipmentResponse.builder()
                .id(s.getId())
                .orderId(order != null ? order.getId() : null)
                .orderCode(order != null ? order.getOrderCode() : null)
                .receiverName(order != null ? order.getReceiverName() : null)
                .receiverPhone(order != null ? order.getReceiverPhone() : null)
                .shippingAddress(order != null ? order.getShippingAddress() : null)
                .carrierCode(carrier != null ? carrier.getCode() : null)
                .carrierName(carrier != null ? carrier.getName() : null)
                .carrierHotline(carrier != null ? carrier.getHotline() : null)
                .partnerRef(s.getPartnerRef())
                .trackingCode(s.getTrackingCode())
                .trackingUrl(trackingUrl(s))
                .serviceType(s.getServiceType())
                .status(s.getStatus())
                .statusLabel(status.map(ShipmentStatus::getLabel).orElse(s.getStatus()))
                .carrierStatusText(s.getCarrierStatusText())
                .cancellable(status.map(ShipmentStatus::isCancellable).orElse(false))
                .shippingFee(s.getShippingFee())
                .weightGrams(s.getWeightGrams())
                .note(s.getNote())
                .shipperName(s.getShipperName())
                .shipperPhone(s.getShipperPhone())
                .estimatedDeliveryDate(s.getEstimatedDeliveryDate())
                .shippedAt(s.getShippedAt())
                .deliveredAt(s.getDeliveredAt())
                .cancelledAt(s.getCancelledAt())
                .cancelReason(s.getCancelReason())
                .lastSyncedAt(s.getLastSyncedAt())
                .createdBy(s.getCreatedBy())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .events(withEvents ? s.getEvents().stream().map(ShipmentMapper::toEventResponse).toList() : null)
                .build();
    }

    // ban cho khach: an thong tin noi bo (ghi chu, nguoi tao, id phia doi tac)
    public static ShipmentResponse toCustomerResponse(Shipment s) {
        ShipmentResponse r = toResponse(s, true);
        r.setNote(null);
        r.setCreatedBy(null);
        r.setPartnerRef(null);
        r.setCancellable(null);
        return r;
    }

    public static ShipmentEventResponse toEventResponse(ShipmentEvent e) {
        return ShipmentEventResponse.builder()
                .id(e.getId())
                .status(e.getStatus())
                .title(e.getTitle())
                .description(e.getDescription())
                .location(e.getLocation())
                .source(e.getSource())
                .occurredAt(e.getOccurredAt())
                .build();
    }

    public static CarrierResponse toCarrierResponse(Carrier c) {
        return CarrierResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .hotline(c.getHotline())
                .build();
    }

    // uu tien link doi tac tra ve, khong co thi dung template cua ĐVVC
    private static String trackingUrl(Shipment s) {
        if (s.getTrackingUrl() != null) {
            return s.getTrackingUrl();
        }
        return s.getCarrier() != null ? s.getCarrier().buildTrackingUrl(s.getTrackingCode()) : null;
    }
}
