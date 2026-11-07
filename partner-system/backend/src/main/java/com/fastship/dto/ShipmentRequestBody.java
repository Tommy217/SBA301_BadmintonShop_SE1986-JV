package com.fastship.dto;

import jakarta.validation.constraints.*;

// shop gui yeu cau tao van don (POST /api/v1/shipment-requests)
public record ShipmentRequestBody(
        @NotNull(message = "Thiếu shopShipmentId.") @Positive Long shopShipmentId,
        @Size(max = 100) String shopOrderId,
        @Size(max = 100) String orderCode,
        @NotBlank(message = "Thiếu tên người nhận.") @Size(max = 150) String receiverName,
        @NotBlank(message = "Thiếu số điện thoại người nhận.") @Size(max = 20) String receiverPhone,
        @NotBlank(message = "Thiếu địa chỉ giao hàng.") @Size(max = 500) String shippingAddress,
        @Pattern(regexp = "standard|fast|express", message = "Gói dịch vụ không hợp lệ.") String serviceType,
        @Positive @Max(50000) Integer packageWeightGram,
        @Size(max = 255) String packageDesc,
        @Size(max = 500) String note) {
}
