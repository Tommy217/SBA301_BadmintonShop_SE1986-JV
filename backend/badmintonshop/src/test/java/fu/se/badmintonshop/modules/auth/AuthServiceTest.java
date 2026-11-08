package fu.se.badmintonshop.modules.auth;

import fu.se.badmintonshop.modules.auth.dto.request.LoginRequest;
import fu.se.badmintonshop.modules.auth.dto.request.RegisterRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AuthResponse;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.repository.RoleRepository;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.security.JwtProperties;
import fu.se.badmintonshop.modules.auth.security.JwtService;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import fu.se.badmintonshop.modules.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        JwtProperties properties = new JwtProperties();
        ReflectionTestUtils.setField(properties, "secretBase64", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(properties, "accessTokenTtlSeconds", 86400L);
        jwtService = new JwtService(properties);

        authService = new AuthServiceImpl(userRepository, roleRepository, passwordEncoder, jwtService);
    }

    @Test
    void authenticatesValidUserAndGeneratesJwtToken() {
        Role customerRole = Role.builder().id(1).code("CUSTOMER").name("Khách hàng").build();
        User user = User.builder()
            .id(1L)
            .email("customer@gmail.com")
            .fullName("Nguyen Van A")
            .passwordHash(passwordEncoder.encode("123456"))
            .status("active")
            .role(customerRole)
            .build();

        when(userRepository.findByEmailIgnoreCase("customer@gmail.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        AuthResponse response = authService.login(LoginRequest.builder()
            .email("customer@gmail.com")
            .password("123456")
            .build());

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getAuthToken());
        assertEquals("customer@gmail.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void rejectsBadCredentialsAndLockedUser() {
        Role customerRole = Role.builder().id(1).code("CUSTOMER").name("Khách hàng").build();
        User lockedUser = User.builder()
            .id(2L)
            .email("locked@gmail.com")
            .fullName("Locked User")
            .passwordHash(passwordEncoder.encode("123456"))
            .status("locked")
            .role(customerRole)
            .build();

        when(userRepository.findByEmailIgnoreCase("locked@gmail.com")).thenReturn(Optional.of(lockedUser));
        when(userRepository.findByEmailIgnoreCase("missing@gmail.com")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
            authService.login(LoginRequest.builder().email("missing@gmail.com").password("123").build()));

        assertThrows(ResponseStatusException.class, () ->
            authService.login(LoginRequest.builder().email("locked@gmail.com").password("123456").build()));
    }

    @Test
    void registersNewCustomerSuccessfully() {
        Role customerRole = Role.builder().id(1).code("CUSTOMER").name("Khách hàng").build();
        when(userRepository.existsByEmailIgnoreCase("newuser@gmail.com")).thenReturn(false);
        when(roleRepository.findByCodeIgnoreCase("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        AuthResponse response = authService.register(RegisterRequest.builder()
            .email("newuser@gmail.com")
            .fullName("New User")
            .password("password123")
            .phone("0912345678")
            .build());

        assertNotNull(response.getAccessToken());
        assertEquals("newuser@gmail.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void rejectsDuplicateEmailOnRegister() {
        when(userRepository.existsByEmailIgnoreCase("existing@gmail.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () ->
            authService.register(RegisterRequest.builder()
                .email("existing@gmail.com")
                .fullName("Existing")
                .password("password")
                .build()));
    }

    @Test
    void returnsProfileForMe() {
        Role adminRole = Role.builder().id(2).code("ADMIN").name("Quản trị viên").build();
        User user = User.builder()
            .id(5L)
            .email("admin@test.com")
            .fullName("Admin")
            .status("active")
            .role(adminRole)
            .build();

        when(userRepository.findWithRoleById(5L)).thenReturn(Optional.of(user));

        CustomUserDetails userDetails = new CustomUserDetails(
            5L,
            "admin@test.com",
            "Admin",
            "hash",
            true,
            false,
            "ADMIN"
        );

        UserResponse profile = authService.me(userDetails);
        assertEquals(5L, profile.getId());
        assertEquals("admin@test.com", profile.getEmail());
        assertEquals("ADMIN", profile.getRole());
    }
}
