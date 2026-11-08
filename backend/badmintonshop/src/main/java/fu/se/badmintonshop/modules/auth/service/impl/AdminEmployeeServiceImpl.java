package fu.se.badmintonshop.modules.auth.service.impl;

import fu.se.badmintonshop.modules.auth.dto.request.CreateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.enums.UserStatus;
import fu.se.badmintonshop.modules.auth.repository.RoleRepository;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.service.AdminEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminEmployeeServiceImpl implements AdminEmployeeService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> ALLOWED_EMPLOYEE_ROLES = Set.of("ADMIN", "MANAGER", "STAFF");

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listEmployees(String keyword, String role, String status, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String cleanRole = (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role.trim())) ? role.trim().toLowerCase(Locale.ROOT) : null;
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim().toLowerCase(Locale.ROOT) : null;

        return userRepository.searchEmployees(cleanKeyword, cleanRole, cleanStatus, pageable)
            .map(this::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getEmployee(Long id) {
        User user = userRepository.findWithRoleById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên."));
        if ("customer".equalsIgnoreCase(user.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không phải là nhân viên nội bộ.");
        }
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createEmployee(CreateEmployeeRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã tồn tại.");
        }

        String roleCode = request.role().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_EMPLOYEE_ROLES.contains(roleCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ cho nhân sự (chỉ chấp nhận ADMIN, MANAGER, STAFF).");
        }

        Role role = roleRepository.findByCodeIgnoreCase(roleCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò '" + roleCode + "' không tồn tại trong hệ thống."));

        User user = User.builder()
            .email(email)
            .fullName(request.fullName().trim())
            .phone(request.phone() != null && !request.phone().isBlank() ? request.phone().trim() : null)
            .gender(request.gender() != null && !request.gender().isBlank() ? request.gender().trim() : null)
            .passwordHash(passwordEncoder.encode(request.password()))
            .status(UserStatus.ACTIVE.getValue())
            .role(role)
            .build();

        user = userRepository.save(user);
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateEmployee(Long id, UpdateEmployeeRequest request, Long currentUserId) {
        User employee = userRepository.findWithRoleById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên."));

        if ("customer".equalsIgnoreCase(employee.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không phải là nhân viên nội bộ.");
        }

        // Self-protection: admin cannot change own role
        if (currentUserId != null && currentUserId.equals(id)) {
            if (request.role() != null && !request.role().trim().equalsIgnoreCase(employee.getRole().getCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự thay đổi vai trò của chính tài khoản đang đăng nhập.");
            }
        }

        if (request.fullName() != null) {
            String fullName = request.fullName().trim();
            if (fullName.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ và tên không được để trống.");
            }
            employee.setFullName(fullName);
        }

        if (request.phone() != null) {
            String phone = request.phone().trim();
            employee.setPhone(phone.isEmpty() ? null : phone);
        }

        if (request.gender() != null) {
            String gender = request.gender().trim();
            employee.setGender(gender.isEmpty() ? null : gender);
        }

        if (request.role() != null && !request.role().isBlank()) {
            String roleCode = request.role().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_EMPLOYEE_ROLES.contains(roleCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ cho nhân sự (chỉ chấp nhận ADMIN, MANAGER, STAFF).");
            }
            Role newRole = roleRepository.findByCodeIgnoreCase(roleCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không tồn tại."));
            employee.setRole(newRole);
        }

        if (request.status() != null && !request.status().isBlank()) {
            String newStatus = request.status().trim().toLowerCase(Locale.ROOT);
            if (!Set.of(UserStatus.ACTIVE.getValue(), UserStatus.LOCKED.getValue()).contains(newStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ (chỉ chấp nhận active hoặc locked).");
            }
            if (currentUserId != null && currentUserId.equals(id) && UserStatus.LOCKED.getValue().equals(newStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự khóa tài khoản của chính mình.");
            }
            employee.setStatus(newStatus);
        }

        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu tối thiểu 6 ký tự.");
            }
            employee.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        employee = userRepository.save(employee);
        return toUserResponse(employee);
    }

    @Override
    @Transactional
    public UserResponse updateEmployeeStatus(Long id, String status, Long currentUserId) {
        User employee = userRepository.findWithRoleById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên."));

        if ("customer".equalsIgnoreCase(employee.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không phải là nhân viên nội bộ.");
        }

        String newStatus = status != null ? status.trim().toLowerCase(Locale.ROOT) : "";
        if (!Set.of(UserStatus.ACTIVE.getValue(), UserStatus.LOCKED.getValue()).contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ (chỉ chấp nhận active hoặc locked).");
        }

        // Self-protection: admin cannot lock own account
        if (currentUserId != null && currentUserId.equals(id) && UserStatus.LOCKED.getValue().equals(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự khóa tài khoản của chính mình.");
        }

        employee.setStatus(newStatus);
        employee = userRepository.save(employee);
        return toUserResponse(employee);
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

