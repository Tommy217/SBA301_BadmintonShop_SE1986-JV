package fu.se.badmintonshop.modules.auth.controller;

import fu.se.badmintonshop.modules.auth.dto.request.ForgotPasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.request.LoginRequest;
import fu.se.badmintonshop.modules.auth.dto.request.RegisterRequest;
import fu.se.badmintonshop.modules.auth.dto.request.ResetPasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AuthResponse;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.security.JwtProperties;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtProperties properties;
    private final fu.se.badmintonshop.modules.auth.service.RefreshTokenService refreshTokens;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        var session = refreshTokens.issue(response);
        ResponseCookie cookie = createAuthCookie(response.getAccessToken(), properties.accessTokenTtlSeconds());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString(), createRefreshCookie(session.refreshToken(), session.maxAge()).toString())
            .body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        var session = refreshTokens.issue(response);
        ResponseCookie cookie = createAuthCookie(response.getAccessToken(), properties.accessTokenTtlSeconds());
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.SET_COOKIE, cookie.toString(), createRefreshCookie(session.refreshToken(), session.maxAge()).toString())
            .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = authService.me(userDetails);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
        @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken) {
        refreshTokens.revoke(refreshToken);
        authService.logout(authHeader);
        ResponseCookie cleanCookie = createAuthCookie("", 0L);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cleanCookie.toString(), createRefreshCookie("", 0).toString())
            .build();
    }

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

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("X-Refresh-Request") String refreshRequest,
        @CookieValue(name = "REFRESH_TOKEN", required = false) String token) {
        var session = refreshTokens.refresh(token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
            createAuthCookie(session.response().getAccessToken(), properties.accessTokenTtlSeconds()).toString(),
            createRefreshCookie(session.refreshToken(), session.maxAge()).toString()).body(session.response());
    }

    private ResponseCookie createRefreshCookie(String token, long seconds) {
        return ResponseCookie.from("REFRESH_TOKEN", token).httpOnly(true)
            .secure(properties.cookieSecure()).sameSite(properties.cookieSameSite())
            .path("/api/auth").maxAge(Duration.ofSeconds(seconds)).build();
    }

    private ResponseCookie createAuthCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(properties.cookieName(), token)
            .httpOnly(true)
            .secure(properties.cookieSecure())
            .sameSite(properties.cookieSameSite())
            .path("/")
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
    }
}

