package fu.se.badmintonshop.modules.auth.controller;

import fu.se.badmintonshop.modules.auth.dto.request.CreateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateStatusRequest;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.service.AdminEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeController {

    private final AdminEmployeeService adminEmployeeService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> listEmployees(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminEmployeeService.listEmployees(keyword, role, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(adminEmployeeService.getEmployee(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminEmployeeService.createEmployee(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateEmployee(
        @PathVariable Long id,
        @RequestBody UpdateEmployeeRequest request,
        @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(adminEmployeeService.updateEmployee(id, request, currentUserId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateStatusRequest request,
        @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(adminEmployeeService.updateEmployeeStatus(id, request.status(), currentUserId));
    }
}

