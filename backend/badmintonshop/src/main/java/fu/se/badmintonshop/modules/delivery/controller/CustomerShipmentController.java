package fu.se.badmintonshop.modules.delivery.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.delivery.dto.ShipmentResponse;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// khach xem van don cua don hang minh (dinh danh qua X-User-Id giong module support ticket)
@RestController
@RequestMapping("/api/orders/{orderId}/shipment")
@RequiredArgsConstructor
public class CustomerShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShipmentResponse>> get(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return shipmentService.getForCustomer(orderId, userId)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                // chua co van don: 200, data rong, de FE hien "chua giao cho don vi van chuyen"
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Đơn hàng chưa có vận đơn", null)));
    }
}
