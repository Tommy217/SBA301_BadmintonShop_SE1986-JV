package fu.se.badmintonshop.modules.delivery.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Trạng thái vận đơn, khớp CHECK của shipments.status và trạng thái của FastShip.
 * <p>
 * Luồng: shop tạo yêu cầu ({@link #REQUESTED}) → đối tác duyệt ({@link #PENDING_PICKUP})
 * hoặc từ chối ({@link #REJECTED}) → đối tác cập nhật hành trình đến trạng thái cuối.
 */
@Getter
@RequiredArgsConstructor
public enum ShipmentStatus {
    REQUESTED("requested", "Chờ đối tác duyệt"),
    REJECTED("rejected", "Đối tác từ chối"),
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
        return this == DELIVERED || this == RETURNED || this == CANCELLED || this == REJECTED;
    }

    // shop chi duoc huy truoc khi buu ta lay hang
    public boolean isCancellable() {
        return this == REQUESTED || this == PENDING_PICKUP;
    }

    // kiem tra buoc chuyen trang thai co hop le khong (webhook, sync và huỷ)
    public boolean canTransitionTo(ShipmentStatus next) {
        return next != null && allowedNext().contains(next);
    }

    private Set<ShipmentStatus> allowedNext() {
        return switch (this) {
            case REQUESTED -> EnumSet.of(PENDING_PICKUP, REJECTED, CANCELLED);
            case PENDING_PICKUP -> EnumSet.of(IN_TRANSIT, CANCELLED);
            case IN_TRANSIT -> EnumSet.of(OUT_FOR_DELIVERY, DELIVERY_FAILED);
            case OUT_FOR_DELIVERY -> EnumSet.of(DELIVERED, DELIVERY_FAILED);
            case DELIVERY_FAILED -> EnumSet.of(OUT_FOR_DELIVERY, RETURNED);
            case DELIVERED, RETURNED, CANCELLED, REJECTED -> EnumSet.noneOf(ShipmentStatus.class);
        };
    }

    public static Optional<ShipmentStatus> fromValue(String value) {
        return Arrays.stream(values()).filter(s -> s.value.equalsIgnoreCase(value)).findFirst();
    }
}
