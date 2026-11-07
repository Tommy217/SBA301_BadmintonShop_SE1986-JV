package com.fastship.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastship.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// API cho shop (/api/v1/shipment-requests, /api/v1/shipments) bat buoc header X-Api-Key
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";
    private final byte[] expected;
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiKeyFilter(String apiKey) {
        this.expected = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean shopApi = path.startsWith("/api/v1/shipment-requests") || path.startsWith("/api/v1/shipments/");
        return !shopApi || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        // so sanh thoi gian hang so, tranh doan khoa theo thoi gian phan hoi
        if (key == null || !MessageDigest.isEqual(expected, key.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            mapper.writeValue(response.getWriter(), ApiResponse.error("Thiếu hoặc sai X-Api-Key."));
            return;
        }
        chain.doFilter(request, response);
    }
}
