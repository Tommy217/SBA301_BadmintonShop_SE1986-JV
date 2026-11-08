# KẾ HOẠCH TÁI CẤU TRÚC CLEAN CODE - PHÂN HỆ AUTHENTICATION
> **Mục tiêu:** Dọn sạch toàn bộ mã chết (dead code), loại bỏ các đoạn code phòng thủ thừa thãi (defensive boilerplate), xóa sạch console log/log.info rác, thống nhất cơ chế ném lỗi rõ ràng cho khách hàng và chuẩn hóa kiến trúc Spring Boot & Java 21.  
> **Tài liệu tham chiếu:** `documents/module/auth/clean-code.md`

---

## 1. ĐÁNH GIÁ 3 NHẬN XÉT CỦA DJ & TOÀN BỘ CODE "RÁC" PHÁT HIỆN

### Nhận xét 1: Hàm `login(String email, String password)` (Dòng 108-125)
* **Đánh giá:** **Chính xác 100% – Đây là DEAD CODE (Mã chết).**
* **Hiện trạng:** Hàm này nhận 2 chuỗi `email`, `password`, tự đóng gói vào `LoginRequest` rồi trả về `Map<String, Object>`.
* **Kiểm tra toàn codebase:** Toàn bộ controller chỉ gọi hàm `login(LoginRequest request)` trả về `AuthResponse`. Không có bất kỳ một class, service hay test nào gọi đến hàm `login(String, String)` này.
* **Hành động:** **Xóa bỏ hoàn toàn** method này khỏi cả `AuthService.java` và `AuthServiceImpl.java`.

---

### Nhận xét 2: Bỏ kiểm tra `user.getRole() == null` và fallback thừa thãi (Dòng 76-79, 189-191)
* **Đánh giá:** **Chính xác 100% – Code phòng thủ thừa (Defensive Overkill).**
* **Hiện trạng:**
  ```java
  if (user.getRole() == null || user.getRole().getCode() == null) {
      throw new IllegalStateException("User ID " + user.getId() + " has no role assigned in database");
  }
  ```
* **Bản chất database & Entity:**
  - Bảng `users` có ràng buộc cứng: `role_id INT NOT NULL REFERENCES roles(id)`.
  - Entity `User.java` khai báo: `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "role_id", nullable = false)`.
  - Mọi user khi lưu hoặc nạp từ DB lên đều **bắt buộc phải có Role**. Không bao giờ có trường hợp `role == null`.
* **Hành động:** **Xóa sạch các đoạn if-check và throw `IllegalStateException` thừa thãi này**. Lấy trực tiếp: `String roleCode = user.getRole().getCode().toUpperCase(Locale.ROOT);`.

---

### Nhận xét 3: Email không tồn tại thì phải báo lỗi + Xóa sạch mọi log trong file
* **Đánh giá:** **Chính xác 100% – Cải thiện trải nghiệm khách hàng & Dọn sạch terminal.**
* **Hiện trạng cũ:** Hàm `forgotPassword` âm thầm `return;` và in ra `log.info(...)`. Khách hàng gõ sai email nhưng web vẫn báo "Đã gửi thành công", khiến khách ngồi chờ email trong vô vọng.
* **Hành động:**
  1. Khi không tìm thấy email: Ném ngay `ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không tồn tại trong hệ thống.")` để Frontend hiển thị thông báo lỗi tức thì cho người dùng.
  2. **Xóa sạch toàn bộ `log.info`, `log.debug`** trong `AuthServiceImpl.java`.
  3. Bỏ luôn annotation `@Slf4j` trên `AuthServiceImpl.java`.

---

### CÁC ĐIỂM "RÁC" KHÁC CẦN DỌN DẸP TIẾP THEO:
1. **Xóa FQCN rác:** Xóa tất cả `fu.se.badmintonshop.modules.auth.entity.User` rải rác trong `login`, `register`, `me`, `forgotPassword`, `resetPassword`, `toUserResponse`. Chỉ cần `import fu.se.badmintonshop.modules.auth.entity.User;` ở đầu file.
2. **Xóa method `@Modifying` thừa trong Repository:** Bỏ `invalidateAllActiveTokensForUser` trong `PasswordResetTokenRepository.java` (đã thống nhất ở bước trước).
3. **Xóa vòng lặp bitwise SHA-256 cổ điển:** Thay bằng `HexFormat.of().formatHex(...)` của Java 21.
4. **Xóa nối chuỗi URL & Regex thủ công:** Thay bằng `UriComponentsBuilder` chuẩn Spring.
5. **Dọn Entity `PasswordResetToken.java`:** Xóa 2 hàm `isExpired()` và `isUsed()`, đưa Entity về chuẩn JPA POJO thuần túy.

---

## 2. CHI TIẾT SỬA ĐỔI TỪNG TỆP TIN

---

### Tệp 1: `AuthService.java`
* **Đường dẫn:** [`backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/service/AuthService.java`](file:///d:/SBA301_BadmintonShop_SE1986-JV/backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/service/AuthService.java)

#### ❌ XÓA:
- Xóa dòng khai báo hàm dead code:
  ```java
  Map<String, Object> login(String email, String password);
  ```

#### ✅ MÃ NGUỒN SAU KHI SỬA:
```java
package fu.se.badmintonshop.modules.auth.service;

import fu.se.badmintonshop.modules.auth.dto.request.ForgotPasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.request.LoginRequest;
import fu.se.badmintonshop.modules.auth.dto.request.RegisterRequest;
import fu.se.badmintonshop.modules.auth.dto.request.ResetPasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AuthResponse;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;

public interface AuthService {

    record User(long id, String email, String fullName, String role, boolean isActive) {}

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    UserResponse me(CustomUserDetails userDetails);

    User user(long id);

    User authenticate(String header);

    void logout(String header);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
```

---

### Tệp 2: `PasswordResetToken.java` (Entity)
* **Đường dẫn:** [`backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/entity/PasswordResetToken.java`](file:///d:/SBA301_BadmintonShop_SE1986-JV/backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/entity/PasswordResetToken.java)

#### ❌ XÓA:
- Xóa 2 method nghiệp vụ `isExpired()` và `isUsed()`.

#### ✅ MÃ NGUỒN SAU KHI SỬA (POJO thuần túy):
```java
package fu.se.badmintonshop.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

### Tệp 3: `PasswordResetTokenRepository.java` (Repository)
* **Đường dẫn:** [`backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/repository/PasswordResetTokenRepository.java`](file:///d:/SBA301_BadmintonShop_SE1986-JV/backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/repository/PasswordResetTokenRepository.java)

#### ❌ XÓA:
- Xóa method `invalidateAllActiveTokensForUser` và các import thừa (`@Modifying`, `@Query`, `@Param`, `LocalDateTime`).

#### ✅ MÃ NGUỒN SAU KHI SỬA (Chuẩn 1 dòng):
```java
package fu.se.badmintonshop.modules.auth.repository;

import fu.se.badmintonshop.modules.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
```

---

### Tệp 4: `AuthServiceImpl.java` (Service Implementation)
* **Đường dẫn:** [`backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/service/impl/AuthServiceImpl.java`](file:///d:/SBA301_BadmintonShop_SE1986-JV/backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/service/impl/AuthServiceImpl.java)

#### ❌ CÁC NỘI DUNG CẦN XÓA HOÀN TOÀN:
1. **Bỏ `@Slf4j`** và tất cả các dòng `log.info(...)`, `log.debug(...)`.
2. **Xóa hàm dead code:** `public Map<String, Object> login(String email, String password)`.
3. **Xóa các đoạn kiểm tra role thừa:**
   ```java
   // XÓA Ở LOGIN:
   if (user.getRole() == null || user.getRole().getCode() == null) {
       throw new IllegalStateException("User ID " + user.getId() + " has no role assigned in database");
   }
   // XÓA Ở ME:
   if (user.getRole() == null || user.getRole().getCode() == null) {
       throw new IllegalStateException("User ID " + user.getId() + " has no role assigned");
   }
   ```
4. **Xóa FQCN rác:** Đổi tất cả `fu.se.badmintonshop.modules.auth.entity.User` thành `User`.
5. **Xóa vòng lặp bitwise băm SHA-256 cũ:** Thay bằng `HexFormat.of().formatHex(...)`.
6. **Xóa nối chuỗi URL thủ công:** Thay bằng `UriComponentsBuilder`.

#### ✅ MÃ NGUỒN HOÀN CHỈNH, TINH GỌN SAU KHI SỬA:
```java
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
import java.util.Locale;
import java.util.Optional;
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
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase(Locale.ROOT) : "";
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

        String roleCode = user.getRole().getCode().trim().toUpperCase(Locale.ROOT);

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
            .expiresIn(86400L)
            .user(toUserResponse(user, roleCode))
            .build();
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
            .expiresIn(86400L)
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

        String roleCode = user.getRole().getCode().trim().toUpperCase(Locale.ROOT);
        return toUserResponse(user, roleCode);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthService.User user(long id) {
        return userRepository.findWithRoleById(id)
            .map(u -> new AuthService.User(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRoleCode().toUpperCase(Locale.ROOT),
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
                Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(username);
                if (optionalUser.isPresent()) {
                    User u = optionalUser.get();
                    if (u.isActive() && !u.isLocked()) {
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
        // Stateless JWT - Cookie and token cleanup handled by AuthController & Client
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase(Locale.ROOT) : "";
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

        String resetUrl = UriComponentsBuilder.fromHttpUrl(frontendUrl != null ? frontendUrl : "http://localhost:5173")
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
```

---

### Tệp 5: `AuthController.java` (Controller)
* **Đường dẫn:** [`backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/controller/AuthController.java`](file:///d:/SBA301_BadmintonShop_SE1986-JV/backend/badmintonshop/src/main/java/fu/se/badmintonshop/modules/auth/controller/AuthController.java)

#### ❌ XÓA:
- Xóa `ResponseEntity<Map<String, String>>` và `Map.of("message", ...)`.

#### ✅ SỬA SANG:
```java
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "Hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới."));
    }
```
*(Nếu giữ `Map<String, String>` để tương thích hoàn toàn với interceptor Axios frontend hiện tại `error.response.data?.message`, message trả về được cô đọng rõ ràng).*

---

### Tệp 6: `AuthServiceTest.java` (Test)
* **Đường dẫn:** [`backend/badmintonshop/src/test/java/fu/se/badmintonshop/modules/auth/AuthServiceTest.java`](file:///d:/SBA301_BadmintonShop_SE1986-JV/backend/badmintonshop/src/test/java/fu/se/badmintonshop/modules/auth/AuthServiceTest.java)

#### ❌ XÓA & SỬA:
1. Xóa verify `invalidateAllActiveTokensForUser`.
2. Bổ sung test case kiểm tra email không tồn tại ném `ResponseStatusException(BAD_REQUEST)` ("Email không tồn tại trong hệ thống.").
3. Xóa test case gọi method `login(String, String)`.

---

## 3. CHECKLIST THỰC THI

- [x] **Bước 1:** Cập nhật `AuthService.java` (Xóa method dead code `login(email, password)`).
- [x] **Bước 2:** Cập nhật `PasswordResetToken.java` (Xóa 2 method nghiệp vụ, đưa về POJO thuần).
- [x] **Bước 3:** Cập nhật `PasswordResetTokenRepository.java` (Xóa method `@Modifying` invalidate, chỉ giữ `findByTokenHash`).
- [x] **Bước 4:** Cập nhật `AuthServiceImpl.java` (Áp dụng toàn bộ code clean: ném lỗi email không tồn tại, xóa log, sạch FQCN, Java 21 `HexFormat`, `UriComponentsBuilder`, xóa check role thừa).
- [x] **Bước 5:** Cập nhật `AuthServiceTest.java` (Xóa verify method cũ, bổ sung test email không tồn tại).
- [x] **Bước 6:** Chạy `mvnw test -Dtest=AuthServiceTest` kiểm chứng build thành công 100%.
