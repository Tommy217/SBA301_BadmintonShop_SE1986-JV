package fu.se.badmintonshop.modules.auth.service;

import fu.se.badmintonshop.modules.auth.dto.request.LoginRequest;
import fu.se.badmintonshop.modules.auth.dto.request.RegisterRequest;
import fu.se.badmintonshop.modules.auth.dto.response.AuthResponse;
import fu.se.badmintonshop.modules.auth.dto.response.UserResponse;
import fu.se.badmintonshop.modules.auth.security.CustomUserDetails;

import java.util.List;
import java.util.Map;

public interface AuthService {

    record User(long id, String email, String fullName, List<String> roles, boolean isActive) {}

    AuthResponse login(LoginRequest request);

    Map<String, Object> login(String email, String password);

    AuthResponse register(RegisterRequest request);

    UserResponse me(CustomUserDetails userDetails);

    User user(long id);

    User authenticate(String header);

    void logout(String header);
}

