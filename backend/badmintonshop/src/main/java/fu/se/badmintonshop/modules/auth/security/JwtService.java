package fu.se.badmintonshop.modules.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtlSeconds();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(properties.secretBase64().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        String role = userDetails.role();
        if (role == null || role.isBlank()) {
            throw new IllegalStateException("Cannot generate token: user details has no role");
        }

        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userDetails.getUsername())
            .claim("userId", userDetails.getId())
            .claim("fullName", userDetails.getFullName())
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(properties.accessTokenTtlSeconds())))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Number userId = claims.get("userId", Number.class);
        return userId != null ? userId.longValue() : null;
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public boolean verifyToken(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            boolean usernameMatches = claims.getSubject().equalsIgnoreCase(userDetails.getUsername());
            boolean notExpired = claims.getExpiration().after(new Date());
            return usernameMatches && notExpired;
        } catch (Exception e) {
            log.debug("Token verification failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
