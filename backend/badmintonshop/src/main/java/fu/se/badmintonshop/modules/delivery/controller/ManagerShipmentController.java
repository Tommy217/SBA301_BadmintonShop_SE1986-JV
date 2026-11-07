package fu.se.badmintonshop.modules.delivery.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.delivery.dto.CancelShipmentRequest;
import fu.se.badmintonshop.modules.delivery.dto.CreateShipmentRequest;
import fu.se.badmintonshop.modules.delivery.dto.PageResponse;
import fu.se.badmintonshop.modules.delivery.dto.ShipmentResponse;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// van don cho manager / staff (X-User-Id duoc TokenFilter gan tu token dang nhap)
@RestController
@RequestMapping("/api/manager/shipments")
@RequiredArgsConstructor
public class ManagerShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShipmentResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.list(status, carrier, q, orderId, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentResponse>> create(
            @Valid @RequestBody CreateShipmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
        ShipmentResponse created = shipmentService.create(request, staffId);
        String message = created.getPartnerRef() == null
                ? "Đã lưu vận đơn nhưng chưa gửi được sang đối tác. Hãy bấm \"Gửi lại\"."
                : created.getTrackingCode() != null
                ? "Đã tạo vận đơn " + created.getTrackingCode()
                : "Đã gửi yêu cầu sang " + created.getCarrierName() + ", chờ đối tác duyệt.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ShipmentResponse>builder()
                        .code(HttpStatus.CREATED.value())
                        .message(message)
                        .data(created)
                        .build());
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<ApiResponse<ShipmentResponse>> resend(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Đã gửi lại yêu cầu cho đối tác", shipmentService.resend(id, staffId)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ShipmentResponse>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelShipmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Đã huỷ vận đơn", shipmentService.cancel(id, request, staffId)));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<ApiResponse<ShipmentResponse>> sync(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Đã đồng bộ trạng thái từ đối tác", shipmentService.sync(id, staffId)));
    }
}
