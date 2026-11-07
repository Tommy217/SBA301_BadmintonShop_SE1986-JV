package com.fastship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// buu ta cap nhat hanh trinh
public record StatusUpdateRequest(
        @NotBlank(message = "Vui lòng chọn trạng thái.") String status,
        @Size(max = 300) String note,
        @Size(max = 200) String location) {
}
