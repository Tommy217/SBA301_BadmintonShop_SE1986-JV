package fu.se.badmintonshop.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống.")
    @Size(max = 150, message = "Tên người nhận không được vượt quá 150 ký tự.")
    private String receiverName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống.")
    @Pattern(regexp = "^(0|\\+84)?[0-9]{9,11}$", message = "Số điện thoại không hợp lệ.")
    private String phone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống.")
    @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự.")
    private String province;

    @NotBlank(message = "Quận/Huyện không được để trống.")
    @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự.")
    private String district;

    @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự.")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống.")
    @Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự.")
    private String street;

    @JsonProperty("isDefault")
    @Builder.Default
    private Boolean isDefault = false;
}
