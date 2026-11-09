package fu.se.badmintonshop.modules.auth.service;

import fu.se.badmintonshop.modules.auth.entity.RefreshToken;
import fu.se.badmintonshop.modules.auth.repository.*;
import fu.se.badmintonshop.modules.auth.security.*;
import fu.se.badmintonshop.modules.auth.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final JwtService jwt;
    @Value("${app.jwt.refresh-token-ttl-seconds:604800}")
    private long ttl;

    public record Session(AuthResponse response, String refreshToken, long maxAge) {}

    @Transactional
    public Session issue(AuthResponse response) {
        RefreshToken token = new RefreshToken();
        token.setUser(users.findById(response.getId()).orElseThrow());
        token.setExpiresAt(Instant.now().plusSeconds(ttl));
        String raw = randomToken();
        token.setTokenHash(hash(raw));
        tokens.save(token);
        return new Session(response, raw, ttl);
    }

    @Transactional
    public Session refresh(String raw) {
        RefreshToken token = tokens.findLocked(hash(raw)).orElseThrow(this::unauthorized);
        var user = token.getUser();
        if (!token.getExpiresAt().isAfter(Instant.now()) || !user.isActive() || user.isLocked()) throw unauthorized();
        var details = new CustomUserDetails(user.getId(), user.getEmail(), user.getFullName(), user.getPasswordHash(), user.isActive(), user.isLocked(), user.getRole().getCode());
        String access = jwt.generateAccessToken(details);
        var response = AuthResponse.builder().id(user.getId()).email(user.getEmail()).fullName(user.getFullName())
            .role(details.role()).authToken(access).accessToken(access).tokenType("Bearer").expiresIn(jwt.accessTokenTtlSeconds()).build();
        String replacement = randomToken();
        token.setTokenHash(hash(replacement));
        tokens.save(token);
        return new Session(response, replacement, Math.max(0, token.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()));
    }

    @Transactional
    public void revoke(String raw) {
        if (raw != null && !raw.isBlank()) tokens.findLocked(hash(raw)).ifPresent(tokens::delete);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        if (raw == null || raw.isBlank()) throw unauthorized();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session expired.");
    }
}
