package fu.se.badmintonshop.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.config.SecurityConfig;
import fu.se.badmintonshop.modules.auth.controller.AdminCustomerController;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateStatusRequest;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.security.JwtAuthenticationFilter;
import fu.se.badmintonshop.modules.auth.security.JwtProperties;
import fu.se.badmintonshop.modules.auth.security.JwtService;
import fu.se.badmintonshop.modules.auth.service.AdminCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCustomerController.class)
@Import({SecurityConfig.class, JwtProperties.class, JwtService.class, JwtAuthenticationFilter.class})
class AdminCustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AdminCustomerService adminCustomerService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private CustomUserDetails adminUserDetails;
    private String authHeader;

    @BeforeEach
    void setUp() {
        adminUserDetails = new CustomUserDetails(
            1L,
            "admin@shop.com",
            "Admin User",
            "hashedpass",
            true,
            false,
            "ADMIN"
        );
        when(userDetailsService.loadUserByUsername("admin@shop.com")).thenReturn(adminUserDetails);
        String token = jwtService.generateAccessToken(adminUserDetails);
        authHeader = "Bearer " + token;
    }

    @Test
    @DisplayName("GET /api/admin/customers - 200 OK")
    void testListCustomers() throws Exception {
        UserResponse user = UserResponse.builder()
            .id(40L)
            .email("customer@shop.com")
            .fullName("Customer Forty")
            .role("CUSTOMER")
            .status("active")
            .build();

        when(adminCustomerService.listCustomers(any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user)));

        mockMvc.perform(get("/api/admin/customers").header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").value("customer@shop.com"))
            .andExpect(jsonPath("$.content[0].role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("PATCH /api/admin/customers/{id}/status - 200 OK")
    void testUpdateCustomerStatus() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest("locked");
        UserResponse response = UserResponse.builder()
            .id(40L)
            .email("customer@shop.com")
            .status("locked")
            .locked(true)
            .build();

        when(adminCustomerService.updateCustomerStatus(eq(40L), eq("locked")))
            .thenReturn(response);

        mockMvc.perform(patch("/api/admin/customers/40/status")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("locked"));
    }
}
