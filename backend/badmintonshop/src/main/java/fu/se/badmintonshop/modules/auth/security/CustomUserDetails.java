package fu.se.badmintonshop.modules.auth.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String password;
    private final boolean active;
    private final boolean locked;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(
        Long id,
        String email,
        String fullName,
        String password,
        boolean active,
        boolean locked,
        String role
    ) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("User role must not be null or blank");
        }
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.active = active;
        this.locked = locked;
        this.role = role.trim().toUpperCase(Locale.ROOT);
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    // Accessors for record style .id(), .role(), .isActive(), .isLocked()
    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLocked() {
        return locked;
    }

    public String role() {
        return role;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
