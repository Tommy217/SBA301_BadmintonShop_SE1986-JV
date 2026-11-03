package fu.se.badmintonshop.modules.catalog.controller;

import fu.se.badmintonshop.modules.catalog.dto.request.VoucherRequest;
import fu.se.badmintonshop.modules.catalog.dto.request.VoucherScopeRequest;
import fu.se.badmintonshop.modules.catalog.dto.response.VoucherResponse;
import fu.se.badmintonshop.modules.catalog.dto.response.VoucherScopeResponse;
import fu.se.badmintonshop.modules.catalog.dto.response.VoucherUsageResponse;
import fu.se.badmintonshop.modules.catalog.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {
    private final VoucherService service;

    // --- Vouchers ---

    @GetMapping("")
    public List<VoucherResponse> listVouchers() {
        return service.listVouchers();
    }

    @GetMapping("/{id}")
    public VoucherResponse getVoucher(@PathVariable Integer id) {
        return service.getVoucher(id);
    }

    @PostMapping("")
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVoucher(request));
    }

    @PutMapping("/{id}")
    public VoucherResponse updateVoucher(@PathVariable Integer id, @Valid @RequestBody VoucherRequest request) {
        return service.updateVoucher(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Integer id) {
        service.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    // --- Voucher Scopes ---

    @GetMapping("/scopes")
    public List<VoucherScopeResponse> listScopes() {
        return service.listScopes();
    }

    @GetMapping("/scopes/{id}")
    public VoucherScopeResponse getScope(@PathVariable Integer id) {
        return service.getScope(id);
    }

    @PostMapping("/scopes")
    public ResponseEntity<VoucherScopeResponse> createScope(@Valid @RequestBody VoucherScopeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createScope(request));
    }

    @PutMapping("/scopes/{id}")
    public VoucherScopeResponse updateScope(@PathVariable Integer id, @Valid @RequestBody VoucherScopeRequest request) {
        return service.updateScope(id, request);
    }

    @DeleteMapping("/scopes/{id}")
    public ResponseEntity<Void> deleteScope(@PathVariable Integer id) {
        service.deleteScope(id);
        return ResponseEntity.noContent().build();
    }

    // --- Voucher Usages (Read-only) ---

    @GetMapping("/usages")
    public List<VoucherUsageResponse> listUsages() {
        return service.listUsages();
    }

    @GetMapping("/usages/{id}")
    public VoucherUsageResponse getUsage(@PathVariable Long id) {
        return service.getUsage(id);
    }
}
