package fu.se.badmintonshop.modules.auth.service.impl;

import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userDetailsService")
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Email cannot be blank");
        }

        User user = userRepository.findByEmailIgnoreCase(username.trim())
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        if (user.getRole() == null || user.getRole().getCode() == null || user.getRole().getCode().isBlank()) {
            throw new IllegalStateException("User ID " + user.getId() + " (" + user.getEmail() + ") has no role assigned in database");
        }

        String roleCode = user.getRole().getCode().trim();
        return new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPasswordHash(),
            user.isActive(),
            user.isLocked(),
            roleCode
        );
    }
}
