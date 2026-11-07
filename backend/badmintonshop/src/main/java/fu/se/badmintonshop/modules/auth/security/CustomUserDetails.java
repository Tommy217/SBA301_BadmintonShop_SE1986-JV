package fu.se.badmintonshop.modules.auth.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String password;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(
        Long id,
        String email,
        String fullName,
        String password,
        boolean active,
        Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.active = active;
        this.authorities = authorities;
    }

    // Accessors for compatibility with record style .id(), .roles(), .isActive()
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

    public List<String> roles() {
        if (authorities == null) return List.of();
        return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
            .toList();
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
        return active;
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

