package fu.se.badmintonshop.modules.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmWebhookRequest {

    @NotBlank(message = "webhookUrl không được để trống")
    private String webhookUrl;
}

