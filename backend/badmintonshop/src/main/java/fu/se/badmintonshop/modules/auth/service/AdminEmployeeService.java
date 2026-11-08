package fu.se.badmintonshop.modules.auth.service;

import fu.se.badmintonshop.modules.auth.dto.request.CreateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminEmployeeService {
    Page<UserResponse> listEmployees(String keyword, String role, String status, Pageable pageable);
    UserResponse getEmployee(Long id);
    UserResponse createEmployee(CreateEmployeeRequest request);
    UserResponse updateEmployee(Long id, UpdateEmployeeRequest request, Long currentUserId);
    UserResponse updateEmployeeStatus(Long id, String status, Long currentUserId);
}

