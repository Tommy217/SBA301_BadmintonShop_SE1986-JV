package fu.se.badmintonshop.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu hiện tại không được để trống.")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống.")
    @Size(min = 6, max = 50, message = "Mật khẩu mới phải có độ dài từ 6 đến 50 ký tự.")
    private String newPassword;

    private String confirmPassword;
}

