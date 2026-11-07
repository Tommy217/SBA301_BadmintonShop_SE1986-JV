package fu.se.badmintonshop.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.config.SecurityConfig;
import fu.se.badmintonshop.modules.auth.controller.ProfileController;
import fu.se.badmintonshop.modules.auth.dto.request.AddressRequest;
import fu.se.badmintonshop.modules.auth.dto.request.ChangePasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateProfileRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AddressResponse;
import fu.se.badmintonshop.modules.auth.dto.response.ProfileResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.security.JwtAuthenticationFilter;
import fu.se.badmintonshop.modules.auth.security.JwtProperties;
import fu.se.badmintonshop.modules.auth.security.JwtService;
import fu.se.badmintonshop.modules.auth.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, JwtProperties.class, JwtService.class, JwtAuthenticationFilter.class})
class ProfileControllerTest {

    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    @Autowired JwtService jwtService;

    @MockitoBean ProfileService profileService;
    @MockitoBean UserDetailsService userDetailsService;

    CustomUserDetails userDetails;
    String authHeader;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(
            10L,
            "profileuser@test.com",
            "Profile User",
            "hashedpass",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(userDetailsService.loadUserByUsername("profileuser@test.com")).thenReturn(userDetails);
        String token = jwtService.generateAccessToken(userDetails);
        authHeader = "Bearer " + token;
    }

    @Test
    void rejectsAnonymousAccessToProfile() throws Exception {
        mvc.perform(get("/api/v1/profile"))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/profile/addresses"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileReturnsOkWhenAuthenticated() throws Exception {
        ProfileResponse profile = ProfileResponse.builder()
            .id(10L)
            .email("profileuser@test.com")
            .fullName("Profile User")
            .roles(List.of("CUSTOMER"))
            .active(true)
            .build();

        when(profileService.getProfile(any())).thenReturn(profile);

        mvc.perform(get("/api/v1/profile").header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.email").value("profileuser@test.com"))
            .andExpect(jsonPath("$.fullName").value("Profile User"));
    }

    @Test
    void updateProfileValidatesAndReturnsOk() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .fullName("Updated Name")
            .phone("0987654321")
            .gender("male")
            .birthDate(LocalDate.of(1995, 5, 20))
            .build();

        ProfileResponse updated = ProfileResponse.builder()
            .id(10L)
            .email("profileuser@test.com")
            .fullName("Updated Name")
            .phone("0987654321")
            .gender("male")
            .build();

        when(profileService.updateProfile(any(), any())).thenReturn(updated);

        mvc.perform(put("/api/v1/profile")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Updated Name"))
            .andExpect(jsonPath("$.phone").value("0987654321"));
    }

    @Test
    void changePasswordReturnsOkOnSuccess() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
            .currentPassword("oldSecret123")
            .newPassword("newSecret456")
            .confirmPassword("newSecret456")
            .build();

        mvc.perform(post("/api/v1/profile/change-password")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công."));
    }

    @Test
    void getAddressesReturnsList() throws Exception {
        AddressResponse addr = AddressResponse.builder()
            .id(1001L)
            .userId(10L)
            .receiverName("Receiver Name")
            .phone("0987654321")
            .province("Hà Nội")
            .district("Cầu Giấy")
            .street("Duy Tân")
            .isDefault(true)
            .build();

        when(profileService.getAddresses(any())).thenReturn(List.of(addr));

        mvc.perform(get("/api/v1/profile/addresses").header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1001))
            .andExpect(jsonPath("$[0].receiverName").value("Receiver Name"))
            .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void addAddressReturnsCreatedStatus() throws Exception {
        AddressRequest request = AddressRequest.builder()
            .receiverName("New Receiver")
            .phone("0987654321")
            .province("Hà Nội")
            .district("Ba Đình")
            .street("Kim Mã")
            .isDefault(true)
            .build();

        AddressResponse created = AddressResponse.builder()
            .id(1002L)
            .userId(10L)
            .receiverName("New Receiver")
            .phone("0987654321")
            .province("Hà Nội")
            .district("Ba Đình")
            .street("Kim Mã")
            .isDefault(true)
            .build();

        when(profileService.addAddress(any(), any())).thenReturn(created);

        mvc.perform(post("/api/v1/profile/addresses")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1002))
            .andExpect(jsonPath("$.receiverName").value("New Receiver"));
    }

    @Test
    void updateAddressReturnsUpdatedData() throws Exception {
        AddressRequest request = AddressRequest.builder()
            .receiverName("Edited Receiver")
            .phone("0987654321")
            .province("Hà Nội")
            .district("Ba Đình")
            .street("Giảng Võ")
            .isDefault(true)
            .build();

        AddressResponse updated = AddressResponse.builder()
            .id(1002L)
            .userId(10L)
            .receiverName("Edited Receiver")
            .phone("0987654321")
            .province("Hà Nội")
            .district("Ba Đình")
            .street("Giảng Võ")
            .isDefault(true)
            .build();

        when(profileService.updateAddress(any(), eq(1002L), any())).thenReturn(updated);

        mvc.perform(put("/api/v1/profile/addresses/1002")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.receiverName").value("Edited Receiver"))
            .andExpect(jsonPath("$.street").value("Giảng Võ"));
    }

    @Test
    void deleteAddressReturnsSuccessMessage() throws Exception {
        mvc.perform(delete("/api/v1/profile/addresses/1002").header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Xóa địa chỉ thành công."));
    }
}
