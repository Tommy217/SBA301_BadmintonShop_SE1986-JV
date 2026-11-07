package com.fastship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ly do tu choi / huy
public record ReasonRequest(
        @NotBlank(message = "Vui lòng nhập lý do.") @Size(max = 300) String reason) {
}
