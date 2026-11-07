package com.fastship.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

// trang thai van don phia FastShip, trung gia tri voi shipments.status cua shop
public enum ShipmentStatus {
    REQUESTED("requested", "Chờ duyệt", "Đã nhận yêu cầu từ shop, chờ FastShip duyệt"),
    REJECTED("rejected", "Từ chối", "FastShip từ chối yêu cầu vận chuyển"),
    PENDING_PICKUP("pending_pickup", "Chờ lấy hàng", "Đã duyệt - Chờ bưu tá đến lấy hàng"),
    IN_TRANSIT("in_transit", "Đang trung chuyển", "Đã lấy hàng - Đang luân chuyển qua kho trung chuyển"),
    OUT_FOR_DELIVERY("out_for_delivery", "Đang phát hàng", "Bưu tá đang trên đường giao cho người nhận"),
    DELIVERED("delivered", "Giao thành công", "Giao hàng thành công - Khách đã ký nhận"),
    DELIVERY_FAILED("delivery_failed", "Giao thất bại", "Giao không thành công - Chờ phát lại hoặc chuyển hoàn"),
    RETURNED("returned", "Đã chuyển hoàn", "Kiện hàng đã được chuyển hoàn về shop"),
    CANCELLED("cancelled", "Đã huỷ", "Vận đơn đã huỷ theo yêu cầu của shop");

    private final String value;
    private final String label;
    private final String defaultText;

    ShipmentStatus(String value, String label, String defaultText) {
        this.value = value;
        this.label = label;
        this.defaultText = defaultText;
    }

    public String getValue() { return value; }
    public String getLabel() { return label; }
    public String getDefaultText() { return defaultText; }

    public boolean isFinal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED || this == REJECTED;
    }

    // bang chuyen trang thai dung chung cho moi API cap nhat (giong module delivery cua shop)
    public boolean canTransitionTo(ShipmentStatus next) {
        return next != null && allowedNext().contains(next);
    }

    public Set<ShipmentStatus> allowedNext() {
        return switch (this) {
            case REQUESTED -> EnumSet.of(PENDING_PICKUP, REJECTED, CANCELLED);
            case PENDING_PICKUP -> EnumSet.of(IN_TRANSIT, CANCELLED);
            case IN_TRANSIT -> EnumSet.of(OUT_FOR_DELIVERY, DELIVERY_FAILED);
            case OUT_FOR_DELIVERY -> EnumSet.of(DELIVERED, DELIVERY_FAILED);
            case DELIVERY_FAILED -> EnumSet.of(OUT_FOR_DELIVERY, RETURNED);
            case DELIVERED, RETURNED, CANCELLED, REJECTED -> EnumSet.noneOf(ShipmentStatus.class);
        };
    }

    // trang thai buu ta cap nhat qua man hinh van don (duyet / tu choi / huy co API rieng)
    public static final Set<ShipmentStatus> COURIER_UPDATES =
            EnumSet.of(IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, DELIVERY_FAILED, RETURNED);

    public static Optional<ShipmentStatus> fromValue(String value) {
        return Arrays.stream(values()).filter(s -> s.value.equalsIgnoreCase(value)).findFirst();
    }
}
