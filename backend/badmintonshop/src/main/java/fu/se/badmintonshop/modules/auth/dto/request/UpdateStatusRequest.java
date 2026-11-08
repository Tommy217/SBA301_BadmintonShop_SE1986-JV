package fu.se.badmintonshop.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateStatusRequest(
    @NotBlank(message = "Trạng thái không được để trống.")
    String status
) {}

