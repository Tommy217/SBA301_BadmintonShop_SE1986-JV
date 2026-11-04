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

// van don cho manager / staff
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.list(status, carrier, q, page, size)));
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ShipmentResponse>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("Đã gửi yêu cầu tạo vận đơn, chờ đối tác duyệt")
                        .data(created)
                        .build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ShipmentResponse>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelShipmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã huỷ vận đơn", shipmentService.cancel(id, request)));
    }
}
