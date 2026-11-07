package com.fastship.controller;

import com.fastship.document.PartnerShipment;
import com.fastship.dto.ApiResponse;
import com.fastship.dto.CalculateFeeRequest;
import com.fastship.service.FeeCalculator;
import com.fastship.service.PartnerShipmentService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

// API cong khai: tra cuu theo ma van don, tinh phi
@RestController
@RequestMapping("/api/v1")
public class TrackingController {

    private final PartnerShipmentService service;

    public TrackingController(PartnerShipmentService service) {
        this.service = service;
    }

    @GetMapping("/track/{trackingCode}")
    public ApiResponse<PartnerShipment> track(@PathVariable String trackingCode) {
        return ApiResponse.ok(service.track(trackingCode));
    }

    @PostMapping("/fees/calculate")
    public ApiResponse<Map<String, Object>> fee(@RequestBody CalculateFeeRequest req) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("serviceType", FeeCalculator.normalizeService(req.getServiceType()));
        res.put("shippingFee", FeeCalculator.fee(req.getServiceType(), req.getWeightGram()));
        res.put("estimatedDeliveryDays", FeeCalculator.estimatedDays(req.getServiceType()));
        res.put("currency", "VND");
        return ApiResponse.ok(res);
    }
}
