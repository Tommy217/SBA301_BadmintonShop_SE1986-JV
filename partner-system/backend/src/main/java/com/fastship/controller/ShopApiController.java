package com.fastship.controller;

import com.fastship.document.PartnerShipment;
import com.fastship.dto.ApiResponse;
import com.fastship.dto.ReasonRequest;
import com.fastship.dto.ShipmentRequestBody;
import com.fastship.service.PartnerShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// API cho Badminton Shop goi (can X-Api-Key, xem ApiKeyFilter)
@RestController
@RequestMapping("/api/v1")
public class ShopApiController {

    private final PartnerShipmentService service;

    public ShopApiController(PartnerShipmentService service) {
        this.service = service;
    }

    @PostMapping("/shipment-requests")
    public ResponseEntity<ApiResponse<PartnerShipment>> submit(@Valid @RequestBody ShipmentRequestBody body) {
        PartnerShipment s = service.submit(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("FastShip đã nhận yêu cầu, chờ duyệt.", s));
    }

    @GetMapping("/shipments/{partnerRef}")
    public ApiResponse<PartnerShipment> get(@PathVariable String partnerRef) {
        return ApiResponse.ok(service.get(partnerRef));
    }

    @PostMapping("/shipments/{partnerRef}/cancel")
    public ApiResponse<PartnerShipment> cancel(@PathVariable String partnerRef, @Valid @RequestBody ReasonRequest body) {
        return ApiResponse.ok("Đã huỷ vận đơn.", service.cancelByShop(partnerRef, body.reason().trim()));
    }
}
