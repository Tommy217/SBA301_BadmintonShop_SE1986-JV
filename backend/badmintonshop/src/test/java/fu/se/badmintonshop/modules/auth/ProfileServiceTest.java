package fu.se.badmintonshop.modules.auth;

import fu.se.badmintonshop.modules.auth.dto.request.AddressRequest;
import fu.se.badmintonshop.modules.auth.dto.request.ChangePasswordRequest;
import fu.se.badmintonshop.modules.auth.dto.request.UpdateProfileRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AddressResponse;
import fu.se.badmintonshop.modules.auth.dto.response.ProfileResponse;
import fu.se.badmintonshop.modules.auth.entity.Address;
import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.repository.AddressRepository;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import fu.se.badmintonshop.modules.auth.service.ProfileService;
import fu.se.badmintonshop.modules.auth.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AddressRepository addressRepository;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    ProfileService profileService;

    CustomUserDetails currentUser;
    User sampleUser;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(userRepository, addressRepository, passwordEncoder);

        currentUser = new CustomUserDetails(
            1L,
            "test@badminton.com",
            "Nguyen Van Test",
            passwordEncoder.encode("oldpass123"),
            true,
            false,
            "CUSTOMER"
        );

        sampleUser = User.builder()
            .id(1L)
            .email("test@badminton.com")
            .fullName("Nguyen Van Test")
            .phone("0987654321")
            .gender("male")
            .birthDate(LocalDate.of(2000, 1, 1))
            .passwordHash(passwordEncoder.encode("oldpass123"))
            .status("active")
            .role(Role.builder().id(1).code("CUSTOMER").name("Khách hàng").build())
            .build();
    }

    @Test
    void getProfileReturnsUserProfile() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));

        ProfileResponse response = profileService.getProfile(currentUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@badminton.com", response.getEmail());
        assertEquals("Nguyen Van Test", response.getFullName());
        assertEquals("0987654321", response.getPhone());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void getProfileThrowsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> profileService.getProfile(currentUser));
    }

    @Test
    void updateProfileUpdatesFieldsSuccessfully() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .fullName("Nguyen Van Mới")
            .phone("0912345678")
            .gender("female")
            .birthDate(LocalDate.of(1999, 12, 31))
            .avatarUrl("https://example.com/avatar.jpg")
            .build();

        ProfileResponse response = profileService.updateProfile(currentUser, request);

        assertEquals("Nguyen Van Mới", response.getFullName());
        assertEquals("0912345678", response.getPhone());
        assertEquals("female", response.getGender());
        assertEquals(LocalDate.of(1999, 12, 31), response.getBirthDate());
        assertEquals("https://example.com/avatar.jpg", response.getAvatarUrl());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void updateProfileRejectsInvalidGender() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .fullName("Nguyen Van A")
            .gender("alien")
            .build();

        assertThrows(ResponseStatusException.class, () -> profileService.updateProfile(currentUser, request));
    }

    @Test
    void changePasswordSuccessfullyUpdatesPassword() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangePasswordRequest request = ChangePasswordRequest.builder()
            .currentPassword("oldpass123")
            .newPassword("newpass456")
            .confirmPassword("newpass456")
            .build();

        profileService.changePassword(currentUser, request);

        assertTrue(passwordEncoder.matches("newpass456", sampleUser.getPasswordHash()));
        verify(userRepository).save(sampleUser);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));

        ChangePasswordRequest request = ChangePasswordRequest.builder()
            .currentPassword("wrongpass")
            .newPassword("newpass456")
            .build();

        assertThrows(ResponseStatusException.class, () -> profileService.changePassword(currentUser, request));
    }

    @Test
    void changePasswordRejectsIdenticalNewPassword() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));

        ChangePasswordRequest request = ChangePasswordRequest.builder()
            .currentPassword("oldpass123")
            .newPassword("oldpass123")
            .build();

        assertThrows(ResponseStatusException.class, () -> profileService.changePassword(currentUser, request));
    }

    @Test
    void changePasswordRejectsMismatchedConfirmPassword() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));

        ChangePasswordRequest request = ChangePasswordRequest.builder()
            .currentPassword("oldpass123")
            .newPassword("newpass456")
            .confirmPassword("newpass789")
            .build();

        assertThrows(ResponseStatusException.class, () -> profileService.changePassword(currentUser, request));
    }

    @Test
    void getAddressesReturnsUserAddresses() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));
        Address addr1 = Address.builder()
            .id(101L)
            .user(sampleUser)
            .receiverName("Tran B")
            .phone("0909090909")
            .province("TP.HCM")
            .district("Quận 1")
            .ward("Phường Bến Nghé")
            .street("123 Lê Lợi")
            .isDefault(true)
            .build();

        when(addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(1L)).thenReturn(List.of(addr1));

        List<AddressResponse> responses = profileService.getAddresses(currentUser);

        assertEquals(1, responses.size());
        assertEquals("Tran B", responses.getFirst().getReceiverName());
        assertTrue(responses.getFirst().isDefault());
        assertEquals("123 Lê Lợi, Phường Bến Nghé, Quận 1, TP.HCM", responses.getFirst().getFullAddress());
    }

    @Test
    void addAddressMakesFirstAddressDefaultAutomatically() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));
        when(addressRepository.existsByUserId(1L)).thenReturn(false);
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(201L);
            return a;
        });

        AddressRequest request = AddressRequest.builder()
            .receiverName("Nguyen Van A")
            .phone("0912345678")
            .province("Hà Nội")
            .district("Cầu Giấy")
            .ward("Dịch Vọng")
            .street("Số 1 Xuân Thủy")
            .isDefault(false)
            .build();

        AddressResponse res = profileService.addAddress(currentUser, request);

        assertNotNull(res);
        assertTrue(res.isDefault(), "First address should default to true even if request passed false");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void addAddressWithDefaultTrueClearsOldDefaults() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));
        when(addressRepository.existsByUserId(1L)).thenReturn(true);

        Address oldDefault = Address.builder().id(101L).user(sampleUser).isDefault(true).build();
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(List.of(oldDefault));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest request = AddressRequest.builder()
            .receiverName("Nguyen Van A")
            .phone("0912345678")
            .province("Hà Nội")
            .district("Cầu Giấy")
            .street("Số 2 Xuân Thủy")
            .isDefault(true)
            .build();

        AddressResponse res = profileService.addAddress(currentUser, request);

        assertTrue(res.isDefault());
        assertFalse(oldDefault.isDefault());
        verify(addressRepository, atLeast(2)).save(any(Address.class));
    }

    @Test
    void updateAddressUpdatesAndProtectsOtherUsers() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));
        Address address = Address.builder()
            .id(101L)
            .user(sampleUser)
            .receiverName("Cu")
            .phone("0900000000")
            .province("Hà Nội")
            .district("Đống Đa")
            .street("Tôn Thất Tùng")
            .isDefault(false)
            .build();

        when(addressRepository.findByIdAndUserId(101L, 1L)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest request = AddressRequest.builder()
            .receiverName("Moi")
            .phone("0911111111")
            .province("Hà Nội")
            .district("Đống Đa")
            .street("Chùa Bộc")
            .isDefault(false)
            .build();

        AddressResponse res = profileService.updateAddress(currentUser, 101L, request);

        assertEquals("Moi", res.getReceiverName());
        assertEquals("0911111111", res.getPhone());
        assertEquals("Chùa Bộc", res.getStreet());

        when(addressRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> profileService.updateAddress(currentUser, 999L, request));
    }

    @Test
    void deleteAddressDeletesAndReassignsDefaultIfWasDefault() {
        when(userRepository.findWithRoleById(1L)).thenReturn(Optional.of(sampleUser));
        Address defaultAddr = Address.builder().id(101L).user(sampleUser).isDefault(true).build();
        Address secondAddr = Address.builder().id(102L).user(sampleUser).isDefault(false).build();

        when(addressRepository.findByIdAndUserId(101L, 1L)).thenReturn(Optional.of(defaultAddr));
        when(addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(1L)).thenReturn(List.of(secondAddr));

        profileService.deleteAddress(currentUser, 101L);

        verify(addressRepository).delete(defaultAddr);
        assertTrue(secondAddr.isDefault());
        verify(addressRepository).save(secondAddr);
    }
}

