package fu.se.badmintonshop.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.config.SecurityConfig;
import fu.se.badmintonshop.modules.auth.controller.AdminEmployeeController;
import fu.se.badmintonshop.modules.auth.dto.request.CreateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateEmployeeRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateStatusRequest;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.security.JwtAuthenticationFilter;
import fu.se.badmintonshop.modules.auth.security.JwtProperties;
import fu.se.badmintonshop.modules.auth.security.JwtService;
import fu.se.badmintonshop.modules.auth.service.AdminEmployeeService;
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

@WebMvcTest(controllers = AdminEmployeeController.class)
@Import({SecurityConfig.class, JwtProperties.class, JwtService.class, JwtAuthenticationFilter.class})
class AdminEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AdminEmployeeService adminEmployeeService;

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
    @DisplayName("GET /api/admin/employees - 200 OK")
    void testListEmployees() throws Exception {
        UserResponse user = UserResponse.builder()
            .id(1L)
            .email("staff@shop.com")
            .fullName("Staff One")
            .role("STAFF")
            .status("active")
            .build();

        when(adminEmployeeService.listEmployees(any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user)));

        mockMvc.perform(get("/api/admin/employees").header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").value("staff@shop.com"))
            .andExpect(jsonPath("$.content[0].role").value("STAFF"));
    }

    @Test
    @DisplayName("POST /api/admin/employees - 201 Created")
    void testCreateEmployee() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            "staff@shop.com", "Staff One", "password123", "0911223344", "male", "STAFF"
        );
        UserResponse response = UserResponse.builder()
            .id(2L)
            .email("staff@shop.com")
            .fullName("Staff One")
            .role("STAFF")
            .status("active")
            .build();

        when(adminEmployeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/employees")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.email").value("staff@shop.com"));
    }

    @Test
    @DisplayName("PUT /api/admin/employees/{id} - 200 OK")
    void testUpdateEmployee() throws Exception {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Staff Updated", null, null, "MANAGER", null, null);
        UserResponse response = UserResponse.builder()
            .id(2L)
            .email("staff@shop.com")
            .fullName("Staff Updated")
            .role("MANAGER")
            .status("active")
            .build();

        when(adminEmployeeService.updateEmployee(eq(2L), any(UpdateEmployeeRequest.class), any()))
            .thenReturn(response);

        mockMvc.perform(put("/api/admin/employees/2")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("PATCH /api/admin/employees/{id}/status - 200 OK")
    void testUpdateEmployeeStatus() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest("locked");
        UserResponse response = UserResponse.builder()
            .id(2L)
            .email("staff@shop.com")
            .status("locked")
            .locked(true)
            .build();

        when(adminEmployeeService.updateEmployeeStatus(eq(2L), eq("locked"), any()))
            .thenReturn(response);

        mockMvc.perform(patch("/api/admin/employees/2/status")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("locked"));
    }
}
