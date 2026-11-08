package fu.se.badmintonshop.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    String email,

    @NotBlank(message = "Họ và tên không được để trống.")
    String fullName,

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự.")
    String password,

    String phone,
    String gender,

    @NotBlank(message = "Vai trò không được để trống.")
    String role
) {}

