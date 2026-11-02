package fu.se.badmintonshop.modules.delivery.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** Trạng thái vận đơn, khớp CHECK của shipments.status và trạng thái của FastShip. */
@Getter
@RequiredArgsConstructor
public enum ShipmentStatus {
    PENDING_PICKUP("pending_pickup", "Chờ lấy hàng"),
    IN_TRANSIT("in_transit", "Đang trung chuyển"),
    OUT_FOR_DELIVERY("out_for_delivery", "Đang phát hàng"),
    DELIVERED("delivered", "Giao thành công"),
    DELIVERY_FAILED("delivery_failed", "Giao không thành công"),
    RETURNED("returned", "Chuyển hoàn về shop"),
    CANCELLED("cancelled", "Đã hủy vận đơn");

    private final String value;
    private final String label;

    public boolean isFinal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }

    public static Optional<ShipmentStatus> fromValue(String value) {
        return Arrays.stream(values()).filter(s -> s.value.equalsIgnoreCase(value)).findFirst();
    }
}