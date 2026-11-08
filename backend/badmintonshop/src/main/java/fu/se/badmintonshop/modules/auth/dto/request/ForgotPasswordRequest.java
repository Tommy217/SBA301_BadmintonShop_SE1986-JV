package fu.se.badmintonshop.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Địa chỉ email không đúng định dạng.")
    private String email;
}

