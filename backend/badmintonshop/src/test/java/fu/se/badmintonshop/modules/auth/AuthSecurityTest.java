package fu.se.badmintonshop.modules.auth;

import fu.se.badmintonshop.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, AuthSecurityTest.Resources.class})
@Import({SecurityConfig.class, AuthSecurityTest.Resources.class})
class AuthSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService auth;
    @RestController static class Resources {
        @GetMapping({"/api/users", "/api/inventory", "/api/manager/shipments", "/api/brands"})
        String resource() { return "ok"; }
        @PostMapping("/api/brands") String write() { return "ok"; }
        @GetMapping("/api/support-tickets") String identity(@RequestHeader("X-User-Id") String id) { return id; }
    }
    @Test void rejectsAnonymousAndInvalidTokensButAllowsPublicCatalog() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header("Authorization", "Bearer mock-fake")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/brands")).andExpect(status().isOk());
    }
    @Test void enforcesDatabaseRolesAndOverridesSpoofedUserHeader() throws Exception {
        for (String role : List.of("ADMIN", "MANAGER", "STAFF", "CUSTOMER")) {
            String header = "Bearer " + role;
            when(auth.authenticate(header)).thenReturn(new AuthService.User(4,"user@test.com","User",List.of(role),true));
            mvc.perform(get("/api/users").header("Authorization",header)).andExpect(status().is(role.equals("ADMIN") ? 200 : 403));
            mvc.perform(get("/api/inventory").header("Authorization",header)).andExpect(status().is(role.equals("CUSTOMER") ? 403 : 200));
            mvc.perform(get("/api/manager/shipments").header("Authorization",header)).andExpect(status().is(role.equals("CUSTOMER") ? 403 : 200));
            mvc.perform(post("/api/brands").header("Authorization",header)).andExpect(status().is(role.equals("CUSTOMER") ? 403 : 200));
            mvc.perform(get("/api/support-tickets").header("Authorization",header).header("X-User-Id","1"))
                .andExpect(status().isOk()).andExpect(content().string("4"));
        }
    }
}
