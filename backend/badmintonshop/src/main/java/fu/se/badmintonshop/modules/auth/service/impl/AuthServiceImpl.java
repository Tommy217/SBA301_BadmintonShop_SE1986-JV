package fu.se.badmintonshop.modules.auth.service.impl;

import fu.se.badmintonshop.modules.auth.dto.request.LoginRequest;
import fu.se.badmintonshop.modules.auth.dto.request.RegisterRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AuthResponse;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.repository.RoleRepository;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.security.JwtService;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase(Locale.ROOT) : "";
        fu.se.badmintonshop.modules.auth.entity.User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng.");
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị khóa.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        List<String> roleCodes = user.getRoles().stream()
            .map(Role::getCode)
            .filter(Objects::nonNull)
            .map(c -> c.toUpperCase(Locale.ROOT))
            .sorted()
            .toList();

        List<GrantedAuthority> authorities = roleCodes.stream()
            .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
            .toList();

        CustomUserDetails userDetails = new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPasswordHash(),
            user.isActive(),
            authorities
        );

        String token = jwtService.generateAccessToken(userDetails);

        UserResponse userResponse = toUserResponse(user, roleCodes);

        return AuthResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roleCodes)
            .authToken(token)
            .accessToken(token)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .user(userResponse)
            .build();
    }

    @Override
    @Transactional
    public Map<String, Object> login(String email, String password) {
        LoginRequest req = LoginRequest.builder()
            .email(email)
            .password(password)
            .build();
        AuthResponse res = login(req);
        return Map.of(
            "id", res.getId(),
            "email", res.getEmail(),
            "fullName", res.getFullName(),
            "roles", res.getRoles(),
            "authToken", res.getAuthToken(),
            "accessToken", res.getAccessToken()
        );
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase(Locale.ROOT) : "";
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng.");
        }

        Role customerRole = roleRepository.findByCodeIgnoreCase("CUSTOMER")
            .orElseGet(() -> roleRepository.save(Role.builder()
                .code("CUSTOMER")
                .name("Khách hàng")
                .build()));

        fu.se.badmintonshop.modules.auth.entity.User user = fu.se.badmintonshop.modules.auth.entity.User.builder()
            .email(email)
            .fullName(request.getFullName().trim())
            .phone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .status("active")
            .roles(new HashSet<>(Set.of(customerRole)))
            .build();

        user = userRepository.save(user);

        List<String> roleCodes = List.of("CUSTOMER");
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

        CustomUserDetails userDetails = new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPasswordHash(),
            true,
            authorities
        );

        String token = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roleCodes)
            .authToken(token)
            .accessToken(token)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .user(toUserResponse(user, roleCodes))
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        fu.se.badmintonshop.modules.auth.entity.User user = userRepository.findWithRolesById(userDetails.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại."));

        List<String> roles = user.getRoles().stream()
            .map(Role::getCode)
            .filter(Objects::nonNull)
            .map(c -> c.toUpperCase(Locale.ROOT))
            .sorted()
            .toList();

        return toUserResponse(user, roles);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthService.User user(long id) {
        return userRepository.findWithRolesById(id)
            .map(u -> new AuthService.User(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRoles().stream()
                    .map(Role::getCode)
                    .filter(Objects::nonNull)
                    .map(c -> c.toUpperCase(Locale.ROOT))
                    .sorted()
                    .toList(),
                u.isActive()
            ))
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthService.User authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        try {
            String username = jwtService.extractUsername(token);
            if (username != null && !username.isBlank()) {
                Optional<fu.se.badmintonshop.modules.auth.entity.User> optionalUser = userRepository.findByEmailIgnoreCase(username);
                if (optionalUser.isPresent()) {
                    fu.se.badmintonshop.modules.auth.entity.User u = optionalUser.get();
                    if (u.isActive()) {
                        return user(u.getId());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void logout(String header) {
        log.debug("User logged out with header: {}", header != null ? "present" : "null");
    }

    private UserResponse toUserResponse(fu.se.badmintonshop.modules.auth.entity.User user, List<String> roleCodes) {
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
            .roles(roleCodes)
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
