package fu.se.badmintonshop.modules.supportticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRequest {

    @NotBlank(message = "result is required (PASSED or FAILED)")
    private String result;

    private String note;

    private Long staffId;
}

