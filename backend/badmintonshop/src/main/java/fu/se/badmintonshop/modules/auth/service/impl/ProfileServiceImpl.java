package fu.se.badmintonshop.modules.auth.service.impl;

import fu.se.badmintonshop.modules.auth.dto.request.AddressRequest;
import fu.se.badmintonshop.modules.auth.dto.request.ChangePasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateProfileRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AddressResponse;
import fu.se.badmintonshop.modules.auth.dto.response.ProfileResponse;
import fu.se.badmintonshop.modules.auth.entity.Address;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.repository.AddressRepository;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(CustomUserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ProfileResponse.fromUser(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest request) {
        User user = getCurrentUser(userDetails);

        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ và tên không được để trống.");
            }
            user.setFullName(fullName);
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            user.setPhone(phone.isEmpty() ? null : phone);
        }

        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }

        if (request.getGender() != null) {
            String gender = request.getGender().trim().toLowerCase(Locale.ROOT);
            if (!gender.isEmpty()) {
                if (!List.of("male", "female", "other").contains(gender)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giới tính không hợp lệ.");
                }
                user.setGender(gender);
            } else {
                user.setGender(null);
            }
        }

        if (request.getAvatarUrl() != null) {
            String avatarUrl = request.getAvatarUrl().trim();
            user.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl);
        }

        User updated = userRepository.save(user);
        return ProfileResponse.fromUser(updated);
    }

    @Override
    @Transactional
    public void changePassword(CustomUserDetails userDetails, ChangePasswordRequest request) {
        User user = getCurrentUser(userDetails);

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không được để trống.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được để trống.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Xác nhận mật khẩu mới không khớp.");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password successfully changed for user id: {}", user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(CustomUserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(user.getId()).stream()
            .map(AddressResponse::fromEntity)
            .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(CustomUserDetails userDetails, AddressRequest request) {
        User user = getCurrentUser(userDetails);

        boolean hasExisting = addressRepository.existsByUserId(user.getId());
        boolean shouldBeDefault = !hasExisting || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault) {
            clearDefaultAddresses(user.getId());
        }

        Address address = Address.builder()
            .user(user)
            .receiverName(request.getReceiverName().trim())
            .phone(request.getPhone().trim())
            .province(request.getProvince().trim())
            .district(request.getDistrict().trim())
            .ward(request.getWard() != null ? request.getWard().trim() : null)
            .street(request.getStreet().trim())
            .isDefault(shouldBeDefault)
            .build();

        Address saved = addressRepository.save(address);
        log.info("Created address id {} for user id {}", saved.getId(), user.getId());
        return AddressResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(CustomUserDetails userDetails, Long addressId, AddressRequest request) {
        User user = getCurrentUser(userDetails);

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ hoặc bạn không có quyền truy cập."));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            clearDefaultAddresses(user.getId());
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault()) && address.isDefault()) {
            address.setDefault(false);
        }

        address.setReceiverName(request.getReceiverName().trim());
        address.setPhone(request.getPhone().trim());
        address.setProvince(request.getProvince().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard() != null ? request.getWard().trim() : null);
        address.setStreet(request.getStreet().trim());

        Address updated = addressRepository.save(address);
        log.info("Updated address id {} for user id {}", updated.getId(), user.getId());
        return AddressResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteAddress(CustomUserDetails userDetails, Long addressId) {
        User user = getCurrentUser(userDetails);

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ hoặc bạn không có quyền truy cập."));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);
        log.info("Deleted address id {} for user id {}", addressId, user.getId());

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(user.getId());
            if (!remaining.isEmpty()) {
                Address first = remaining.getFirst();
                first.setDefault(true);
                addressRepository.save(first);
                log.info("Set address id {} as new default for user id {}", first.getId(), user.getId());
            }
        }
    }

    private User getCurrentUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực tài khoản.");
        }

        return userRepository.findWithRoleById(userDetails.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
    }

    private void clearDefaultAddresses(Long userId) {
        List<Address> defaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);
        for (Address addr : defaults) {
            addr.setDefault(false);
            addressRepository.save(addr);
        }
    }
}

