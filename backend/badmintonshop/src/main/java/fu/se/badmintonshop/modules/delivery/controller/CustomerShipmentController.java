package fu.se.badmintonshop.modules.delivery.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.delivery.dto.ShipmentResponse;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// khach xem van don cua don hang minh (X-User-Id duoc TokenFilter gan tu token dang nhap)
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/orders/{orderId}/shipment")
    public ResponseEntity<ApiResponse<ShipmentResponse>> get(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return shipmentService.getForCustomer(orderId, userId)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                // chua co van don: 200, data rong, de FE hien "chua giao cho don vi van chuyen"
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Đơn hàng chưa có vận đơn", null)));
    }

    @PostMapping("/orders/{orderId}/shipment/sync")
    public ResponseEntity<ApiResponse<ShipmentResponse>> sync(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật hành trình mới nhất",
                shipmentService.syncForCustomer(orderId, userId)));
    }

    @GetMapping("/my/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> mine(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.listForCustomer(userId)));
    }
}
