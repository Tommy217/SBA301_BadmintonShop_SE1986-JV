package fu.se.badmintonshop.modules.auth.controller;

import fu.se.badmintonshop.modules.auth.dto.request.UpdateStatusRequest;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.service.AdminCustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> listCustomers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminCustomerService.listCustomers(keyword, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(adminCustomerService.getCustomer(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(adminCustomerService.updateCustomerStatus(id, request.status()));
    }
}

