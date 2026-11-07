package fu.se.badmintonshop.modules.auth.service.impl;

import fu.se.badmintonshop.modules.auth.entity.Role;
import fu.se.badmintonshop.modules.auth.entity.User;
import fu.se.badmintonshop.modules.auth.repository.UserRepository;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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

        Set<Role> roles = user.getRoles();
        List<GrantedAuthority> authorities = roles.stream()
            .map(role -> {
                String code = role.getCode();
                if (code == null) code = "CUSTOMER";
                String upper = code.trim().toUpperCase(Locale.ROOT);
                if (!upper.startsWith("ROLE_")) {
                    upper = "ROLE_" + upper;
                }
                return (GrantedAuthority) new SimpleGrantedAuthority(upper);
            })
            .toList();

        return new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPasswordHash(),
            user.isActive(),
            authorities
        );
    }
}

