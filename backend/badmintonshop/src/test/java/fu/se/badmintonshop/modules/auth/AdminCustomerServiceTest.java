package fu.se.badmintonshop.modules.auth;

import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.enums.UserStatus;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.service.impl.AdminCustomerServiceImpl;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCustomerServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminCustomerServiceImpl adminCustomerService;

    private Role customerRole;
    private Role staffRole;
    private User customerUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(4).code("customer").name("Khách hàng").build();
        staffRole = Role.builder().id(3).code("staff").name("Nhân viên").build();

        customerUser = User.builder()
            .id(40L)
            .email("customer@shop.com")
            .fullName("Customer Forty")
            .role(customerRole)
            .status(UserStatus.ACTIVE.getValue())
            .build();

        staffUser = User.builder()
            .id(10L)
            .email("staff@shop.com")
            .fullName("Staff Ten")
            .role(staffRole)
            .status(UserStatus.ACTIVE.getValue())
            .build();
    }

    @Test
    @DisplayName("listCustomers returns page of customer responses")
    void testListCustomers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(customerUser), pageable, 1);
        when(userRepository.searchCustomers(null, null, pageable)).thenReturn(page);

        Page<UserResponse> result = adminCustomerService.listCustomers(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getEmail()).isEqualTo("customer@shop.com");
    }

    @Test
    @DisplayName("updateCustomerStatus updates status of customer successfully")
    void testUpdateCustomerStatus_Success() {
        when(userRepository.findWithRoleById(40L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenReturn(customerUser);

        UserResponse res = adminCustomerService.updateCustomerStatus(40L, "locked");

        assertThat(customerUser.getStatus()).isEqualTo("locked");
    }

    @Test
    @DisplayName("updateCustomerStatus fails if user is an employee, not a customer")
    void testUpdateCustomerStatus_FailsForNonCustomer() {
        when(userRepository.findWithRoleById(10L)).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> adminCustomerService.updateCustomerStatus(10L, "locked"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Tài khoản không phải là khách hàng");
    }
}

