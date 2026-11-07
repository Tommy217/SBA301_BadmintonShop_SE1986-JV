package fu.se.badmintonshop.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống.")
    @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự.")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự.")
    @Pattern(regexp = "^(0|\\+84)?[0-9]{9,11}$|^$", message = "Số điện thoại không hợp lệ.")
    private String phone;

    @Past(message = "Ngày sinh phải ở trong quá khứ.")
    private LocalDate birthDate;

    @Pattern(regexp = "^(male|female|other)?$", message = "Giới tính phải là male, female hoặc other.")
    private String gender;

    @Size(max = 500, message = "Đường dẫn ảnh đại diện không được vượt quá 500 ký tự.")
    private String avatarUrl;
}

