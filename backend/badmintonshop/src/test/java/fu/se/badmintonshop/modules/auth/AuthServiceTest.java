package fu.se.badmintonshop.modules.auth;

import fu.se.badmintonshop.modules.auth.dto.request.ForgotPasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.request.LoginRequest;
import fu.se.badmintonshop.modules.auth.dto.request.RegisterRequest;
import fu.se.badmintonshop.modules.auth.dto.request.ResetPasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AuthResponse;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.entity.PasswordResetToken;
import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.repository.PasswordResetTokenRepository;
import fu.se.badmintonshop.modules.auth.repository.RoleRepository;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.security.JwtProperties;
import fu.se.badmintonshop.modules.auth.security.JwtService;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import fu.se.badmintonshop.modules.auth.service.EmailService;
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

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

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

        authService = new AuthServiceImpl(userRepository, roleRepository, passwordEncoder, jwtService, passwordResetTokenRepository, emailService);
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

    @Test
    void forgotPasswordGeneratesTokenAndCallsEmailServiceWhenUserExists() {
        Role customerRole = Role.builder().id(1).code("CUSTOMER").name("Khách hàng").build();
        User user = User.builder()
            .id(1L)
            .email("customer@gmail.com")
            .fullName("Nguyen Van A")
            .status("active")
            .role(customerRole)
            .build();

        when(userRepository.findByEmailIgnoreCase("customer@gmail.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(ForgotPasswordRequest.builder()
            .email("customer@gmail.com")
            .build());

        verify(passwordResetTokenRepository).save(any());
        verify(emailService).sendPasswordResetEmail(
            eq("customer@gmail.com"),
            eq("Nguyen Van A"),
            contains("/reset-password?token=")
        );
    }

    @Test
    void forgotPasswordThrowsExceptionWhenEmailNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@gmail.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.forgotPassword(ForgotPasswordRequest.builder().email("missing@gmail.com").build()));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Email không tồn tại trong hệ thống.", ex.getReason());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void forgotPasswordThrowsExceptionWhenEmailBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.forgotPassword(ForgotPasswordRequest.builder().email("  ").build()));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Email không được để trống.", ex.getReason());
    }

    @Test
    void forgotPasswordThrowsExceptionWhenUserLocked() {
        Role customerRole = Role.builder().id(1).code("CUSTOMER").name("Khách hàng").build();
        User lockedUser = User.builder()
            .id(1L)
            .email("locked@gmail.com")
            .fullName("Locked User")
            .status("locked")
            .role(customerRole)
            .build();

        when(userRepository.findByEmailIgnoreCase("locked@gmail.com")).thenReturn(Optional.of(lockedUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            authService.forgotPassword(ForgotPasswordRequest.builder().email("locked@gmail.com").build()));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.", ex.getReason());
    }

    @Test
    void resetPasswordUpdatesPasswordAndMarksTokenUsed() {
        Role customerRole = Role.builder().id(1).code("CUSTOMER").name("Khách hàng").build();
        User user = User.builder()
            .id(1L)
            .email("customer@gmail.com")
            .fullName("Nguyen Van A")
            .passwordHash("old_hash")
            .status("active")
            .role(customerRole)
            .build();

        PasswordResetToken token = PasswordResetToken.builder()
            .id(10L)
            .user(user)
            .tokenHash("some_hash")
            .expiresAt(java.time.LocalDateTime.now().plusMinutes(15))
            .build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        authService.resetPassword(ResetPasswordRequest.builder()
            .token("test-token-uuid")
            .newPassword("newPassword123")
            .build());

        assertNotNull(token.getUsedAt());
        assertTrue(passwordEncoder.matches("newPassword123", user.getPasswordHash()));
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPasswordRejectsExpiredOrUsedToken() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
            .id(1L)
            .tokenHash("some_hash")
            .expiresAt(java.time.LocalDateTime.now().minusMinutes(5))
            .build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        assertThrows(ResponseStatusException.class, () ->
            authService.resetPassword(ResetPasswordRequest.builder()
                .token("raw-token-1")
                .newPassword("newPass123")
                .build()));

        PasswordResetToken usedToken = PasswordResetToken.builder()
            .id(2L)
            .tokenHash("some_hash")
            .expiresAt(java.time.LocalDateTime.now().plusMinutes(15))
            .usedAt(java.time.LocalDateTime.now())
            .build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));

        assertThrows(ResponseStatusException.class, () ->
            authService.resetPassword(ResetPasswordRequest.builder()
                .token("raw-token-2")
                .newPassword("newPass123")
                .build()));
    }
}
