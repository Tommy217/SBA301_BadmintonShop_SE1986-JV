package fu.se.badmintonshop.modules.auth;

import fu.se.badmintonshop.modules.auth.dto.request.CreateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.enums.UserStatus;
import fu.se.badmintonshop.modules.auth.repository.RoleRepository;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.service.impl.AdminEmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminEmployeeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminEmployeeServiceImpl adminEmployeeService;

    private Role staffRole;
    private Role adminRole;
    private Role customerRole;
    private User staffUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        staffRole = Role.builder().id(3).code("staff").name("Nhân viên").build();
        adminRole = Role.builder().id(1).code("admin").name("Quản trị viên").build();
        customerRole = Role.builder().id(4).code("customer").name("Khách hàng").build();

        staffUser = User.builder()
            .id(10L)
            .email("staff@shop.com")
            .fullName("Staff Ten")
            .role(staffRole)
            .status(UserStatus.ACTIVE.getValue())
            .build();

        adminUser = User.builder()
            .id(1L)
            .email("admin@shop.com")
            .fullName("Admin One")
            .role(adminRole)
            .status(UserStatus.ACTIVE.getValue())
            .build();
    }

    @Test
    @DisplayName("listEmployees returns page of internal employee responses")
    void testListEmployees() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(staffUser), pageable, 1);
        when(userRepository.searchEmployees(null, null, null, pageable)).thenReturn(page);

        Page<UserResponse> result = adminEmployeeService.listEmployees(null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getEmail()).isEqualTo("staff@shop.com");
    }

    @Test
    @DisplayName("createEmployee successfully creates employee with encrypted password")
    void testCreateEmployee_Success() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            "newstaff@shop.com", "New Staff", "pass123", "0911223344", "male", "STAFF"
        );

        when(userRepository.existsByEmailIgnoreCase("newstaff@shop.com")).thenReturn(false);
        when(roleRepository.findByCodeIgnoreCase("STAFF")).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("pass123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        UserResponse res = adminEmployeeService.createEmployee(request);

        assertThat(res.getId()).isEqualTo(20L);
        assertThat(res.getEmail()).isEqualTo("newstaff@shop.com");
        assertThat(res.getRole()).isEqualTo("STAFF");
    }

    @Test
    @DisplayName("createEmployee fails if role is CUSTOMER")
    void testCreateEmployee_FailsForCustomerRole() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            "newuser@shop.com", "New User", "pass123", null, null, "CUSTOMER"
        );

        assertThatThrownBy(() -> adminEmployeeService.createEmployee(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("chỉ chấp nhận ADMIN, MANAGER, STAFF");
    }

    @Test
    @DisplayName("updateEmployee blocks self-role change for current admin")
    void testUpdateEmployee_BlocksSelfRoleChange() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(adminUser));

        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Admin Renamed", null, null, "STAFF", null, null);

        assertThatThrownBy(() -> adminEmployeeService.updateEmployee(1L, request, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Không thể tự thay đổi vai trò");
    }

    @Test
    @DisplayName("updateEmployeeStatus blocks self-lock for current admin")
    void testUpdateEmployeeStatus_BlocksSelfLock() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminEmployeeService.updateEmployeeStatus(1L, "locked", 1L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Không thể tự khóa tài khoản của chính mình");
    }

    @Test
    @DisplayName("updateEmployeeStatus unlocks non-self employee")
    void testUpdateEmployeeStatus_Success() {
        when(userRepository.findWithRoleById(10L)).thenReturn(Optional.of(staffUser));
        when(userRepository.save(any(User.class))).thenReturn(staffUser);

        UserResponse res = adminEmployeeService.updateEmployeeStatus(10L, "locked", 1L);
        assertThat(staffUser.getStatus()).isEqualTo("locked");
    }
}

