package fu.se.badmintonshop.modules.delivery.controller;

import fu.se.badmintonshop.modules.common.dto.ApiResponse;
import fu.se.badmintonshop.modules.delivery.dto.CarrierResponse;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// danh sach don vi van chuyen dang hoat dong (cho modal tao van don)
@RestController
@RequestMapping("/api/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final ShipmentService shipmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarrierResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.activeCarriers()));
    }
}
