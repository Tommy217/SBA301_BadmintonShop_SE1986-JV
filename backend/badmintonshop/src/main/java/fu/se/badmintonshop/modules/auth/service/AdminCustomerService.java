package fu.se.badmintonshop.modules.auth.service;

import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCustomerService {
    Page<UserResponse> listCustomers(String keyword, String status, Pageable pageable);
    UserResponse getCustomer(Long id);
    UserResponse updateCustomerStatus(Long id, String status);
}

