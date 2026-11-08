package fu.se.badmintonshop.modules.auth.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
    String fullName,
    String phone,
    String gender,
    String role,
    String status,
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự.")
    String password
) {}
