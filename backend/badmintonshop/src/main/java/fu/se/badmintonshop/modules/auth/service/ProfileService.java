package fu.se.badmintonshop.modules.auth.service;

import fu.se.badmintonshop.modules.auth.dto.request.AddressRequest;
import fu.se.badmintonshop.modules.auth.dto.request.ChangePasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateProfileRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AddressResponse;
import fu.se.badmintonshop.modules.auth.dto.response.ProfileResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;

import java.util.List;

public interface ProfileService {

    ProfileResponse getProfile(CustomUserDetails userDetails);

    ProfileResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest request);

    void changePassword(CustomUserDetails userDetails, ChangePasswordRequest request);

    List<AddressResponse> getAddresses(CustomUserDetails userDetails);

    AddressResponse addAddress(CustomUserDetails userDetails, AddressRequest request);

    AddressResponse updateAddress(CustomUserDetails userDetails, Long addressId, AddressRequest request);

    void deleteAddress(CustomUserDetails userDetails, Long addressId);
}

