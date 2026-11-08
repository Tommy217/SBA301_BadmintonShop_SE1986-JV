package fu.se.badmintonshop.modules.auth;

import fu.se.badmintonshop.config.SecurityConfig;
import fu.se.badmintonshop.modules.auth.controller.AuthController;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.security.JwtAuthenticationFilter;
import fu.se.badmintonshop.modules.auth.security.JwtProperties;
import fu.se.badmintonshop.modules.auth.security.JwtService;
import fu.se.badmintonshop.modules.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, AuthSecurityTest.Resources.class})
@Import({SecurityConfig.class, JwtProperties.class, JwtService.class, JwtAuthenticationFilter.class, AuthSecurityTest.Resources.class})
class AuthSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService auth;
    @MockitoBean UserDetailsService userDetailsService;
    @Autowired JwtService jwtService;

    @RestController static class Resources {
        @GetMapping({"/api/users", "/api/inventory", "/api/manager/shipments", "/api/brands"})
        String resource() { return "ok"; }
        @PostMapping("/api/brands") String write() { return "ok"; }
        @PostMapping("/api/webhooks/carriers/fastship") String carrierWebhook() { return "ok"; }
        @GetMapping("/api/support-tickets") String identity(@RequestHeader("X-User-Id") String id) { return id; }
    }

    @Test void rejectsAnonymousAndInvalidTokensButAllowsPublicCatalog() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header("Authorization", "Bearer mock-fake")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/brands")).andExpect(status().isOk());
        // webhook doi tac khong dang nhap, xac thuc bang chu ky HMAC trong controller
        mvc.perform(post("/api/webhooks/carriers/fastship")).andExpect(status().isOk());
    }

    @Test void deletedUserTokenReturnsUnauthorized() throws Exception {
        var user = new CustomUserDetails(99L, "deleted@test.com", "Deleted", "", true, List.of());
        String token = jwtService.generateAccessToken(user);
        when(userDetailsService.loadUserByUsername("deleted@test.com"))
            .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/brands").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test void enforcesDatabaseRolesAndOverridesSpoofedUserHeader() throws Exception {
        for (String role : List.of("ADMIN", "MANAGER", "STAFF", "CUSTOMER")) {
            CustomUserDetails userDetails = new CustomUserDetails(
                4L,
                "user@test.com",
                "User",
                "123",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            String token = jwtService.generateAccessToken(userDetails);
            String header = "Bearer " + token;

            mvc.perform(get("/api/users").header("Authorization", header)).andExpect(status().is(role.equals("ADMIN") ? 200 : 403));
            mvc.perform(get("/api/inventory").header("Authorization", header)).andExpect(status().is(role.equals("CUSTOMER") ? 403 : 200));
            mvc.perform(get("/api/manager/shipments").header("Authorization", header)).andExpect(status().is(role.equals("CUSTOMER") ? 403 : 200));
            mvc.perform(post("/api/brands").header("Authorization", header)).andExpect(status().is(role.equals("CUSTOMER") ? 403 : 200));
            mvc.perform(get("/api/support-tickets").header("Authorization", header).header("X-User-Id", "1"))
                .andExpect(status().isOk()).andExpect(content().string("4"));
        }
    }
}
