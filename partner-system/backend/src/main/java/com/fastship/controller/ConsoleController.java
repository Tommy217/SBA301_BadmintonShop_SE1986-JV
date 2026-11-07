package com.fastship.controller;

import com.fastship.document.PartnerShipment;
import com.fastship.dto.ApiResponse;
import com.fastship.dto.ApproveRequest;
import com.fastship.dto.ReasonRequest;
import com.fastship.dto.StatusUpdateRequest;
import com.fastship.service.PartnerShipmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// API cho man hinh nhan vien FastShip (demo, chua dang nhap)
@RestController
@RequestMapping("/api/v1/console/shipments")
public class ConsoleController {

    private final PartnerShipmentService service;

    public ConsoleController(PartnerShipmentService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<PartnerShipment>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<PartnerShipment> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<PartnerShipment> approve(@PathVariable String id, @Valid @RequestBody(required = false) ApproveRequest body) {
        return ApiResponse.ok("Đã duyệt và cấp mã vận đơn.", service.approve(id, body));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<PartnerShipment> reject(@PathVariable String id, @Valid @RequestBody ReasonRequest body) {
        return ApiResponse.ok("Đã từ chối yêu cầu.", service.reject(id, body.reason().trim()));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<PartnerShipment> updateStatus(@PathVariable String id, @Valid @RequestBody StatusUpdateRequest body) {
        return ApiResponse.ok("Đã cập nhật hành trình.", service.updateStatus(id, body));
    }
}
