package fu.se.badmintonshop.modules.auth.service.impl;

import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.enums.UserStatus;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.service.AdminCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listCustomers(String keyword, String status, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim().toLowerCase(Locale.ROOT) : null;

        return userRepository.searchCustomers(cleanKeyword, cleanStatus, pageable)
            .map(this::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCustomer(Long id) {
        User user = userRepository.findWithRoleById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng."));
        if (!"customer".equalsIgnoreCase(user.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không phải là khách hàng.");
        }
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateCustomerStatus(Long id, String status) {
        User customer = userRepository.findWithRoleById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng."));

        if (!"customer".equalsIgnoreCase(customer.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không phải là khách hàng.");
        }

        String newStatus = status != null ? status.trim().toLowerCase(Locale.ROOT) : "";
        if (!Set.of(UserStatus.ACTIVE.getValue(), UserStatus.LOCKED.getValue()).contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ (chỉ chấp nhận active hoặc locked).");
        }

        customer.setStatus(newStatus);
        customer = userRepository.save(customer);
        return toUserResponse(customer);
    }

    private UserResponse toUserResponse(User user) {
        if (user.getRole() == null || user.getRole().getCode() == null) {
            throw new IllegalStateException("User ID " + user.getId() + " has no role assigned");
        }

        String roleCode = user.getRole().getCode().toUpperCase(Locale.ROOT);
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .gender(user.getGender())
            .birthDate(user.getBirthDate())
            .avatarUrl(user.getAvatarUrl())
            .status(user.getStatus())
            .active(user.isActive())
            .locked(user.isLocked())
            .role(roleCode)
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .build();
    }
}

