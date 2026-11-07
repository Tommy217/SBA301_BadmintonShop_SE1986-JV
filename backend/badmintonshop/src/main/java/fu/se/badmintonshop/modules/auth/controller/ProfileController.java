package fu.se.badmintonshop.modules.auth.controller;

import fu.se.badmintonshop.modules.auth.dto.request.AddressRequest;
import fu.se.badmintonshop.modules.auth.dto.request.ChangePasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateProfileRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AddressResponse;
import fu.se.badmintonshop.modules.auth.dto.response.ProfileResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/profile", "/api/v1/profile"})
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ProfileResponse response = profileService.getProfile(userDetails);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        ProfileResponse response = profileService.updateProfile(userDetails, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        profileService.changePassword(userDetails, request);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công."));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AddressResponse> addresses = profileService.getAddresses(userDetails);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse address = profileService.addAddress(userDetails, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long id,
        @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse address = profileService.updateAddress(userDetails, id, request);
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Map<String, String>> deleteAddress(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long id
    ) {
        profileService.deleteAddress(userDetails, id);
        return ResponseEntity.ok(Map.of("message", "Xóa địa chỉ thành công."));
    }
}

