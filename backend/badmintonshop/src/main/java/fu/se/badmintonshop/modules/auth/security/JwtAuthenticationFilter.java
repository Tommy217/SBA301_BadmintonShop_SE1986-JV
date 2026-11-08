package fu.se.badmintonshop.modules.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && !token.isBlank()) {
            try {
                String username = jwtService.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);

                    if (jwtService.verifyToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        // Backward compatibility for existing controllers reading X-User-Id header
                        final String userIdStr = Long.toString(userDetails.getId());
                        request = new HttpServletRequestWrapper(request) {
                            @Override
                            public String getHeader(String name) {
                                return "X-User-Id".equalsIgnoreCase(name) ? userIdStr : super.getHeader(name);
                            }

                            @Override
                            public Enumeration<String> getHeaders(String name) {
                                return "X-User-Id".equalsIgnoreCase(name)
                                    ? Collections.enumeration(List.of(userIdStr))
                                    : super.getHeaders(name);
                            }
                        };
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                log.debug("JWT verification failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("ACCESS_TOKEN".equalsIgnoreCase(cookie.getName()) || "authToken".equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}

