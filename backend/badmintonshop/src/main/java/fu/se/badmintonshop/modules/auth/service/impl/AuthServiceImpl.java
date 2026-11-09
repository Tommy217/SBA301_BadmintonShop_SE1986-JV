package fu.se.badmintonshop.modules.auth.service.impl;

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
import fu.se.badmintonshop.modules.auth.security.JwtService;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import fu.se.badmintonshop.modules.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${shop.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.auth.reset-token-ttl-minutes:15}")
    private int resetTokenTtlMinutes;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng.");
        }

        if (user.isLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không hoạt động.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String roleCode = user.getRole().getCode().trim().toUpperCase();

        CustomUserDetails userDetails = new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPasswordHash(),
            user.isActive(),
            user.isLocked(),
            roleCode
        );

        String token = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(roleCode)
            .authToken(token)
            .accessToken(token)
            .tokenType("Bearer")
            .expiresIn(jwtService.accessTokenTtlSeconds())
            .user(toUserResponse(user, roleCode))
            .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng.");
        }

        Role customerRole = roleRepository.findByCodeIgnoreCase("CUSTOMER")
            .orElseThrow(() -> new IllegalStateException("Critical database error: Role 'CUSTOMER' does not exist in database"));

        User user = User.builder()
            .email(email)
            .fullName(request.getFullName().trim())
            .phone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .status("active")
            .role(customerRole)
            .build();

        user = userRepository.save(user);

        String roleCode = "CUSTOMER";

        CustomUserDetails userDetails = new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPasswordHash(),
            true,
            false,
            roleCode
        );

        String token = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(roleCode)
            .authToken(token)
            .accessToken(token)
            .tokenType("Bearer")
            .expiresIn(jwtService.accessTokenTtlSeconds())
            .user(toUserResponse(user, roleCode))
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        User user = userRepository.findWithRoleById(userDetails.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại."));

        String roleCode = user.getRole().getCode().trim().toUpperCase();
        return toUserResponse(user, roleCode);
    }

    @Override
    public void logout(String header) {
        // Stateless JWT - Cookie and token cleanup handled by AuthController & Client
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không tồn tại trong hệ thống."));

        if (user.isLocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(LocalDateTime.now().plusMinutes(resetTokenTtlMinutes))
            .build();

        passwordResetTokenRepository.save(resetToken);

        String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/reset-password")
            .queryParam("token", rawToken)
            .toUriString();

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetUrl);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String rawToken = request.getToken() != null ? request.getToken().trim() : "";
        if (rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ.");
        }

        String tokenHash = hashToken(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));

        boolean isInvalid = resetToken.getUsedAt() != null || LocalDateTime.now().isAfter(resetToken.getExpiresAt());
        if (isInvalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }

        User user = resetToken.getUser();
        if (user == null || !user.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private UserResponse toUserResponse(User user, String roleCode) {
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
