package fu.se.badmintonshop.modules.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BrandRequest {
    @NotBlank(message = "Tên thương hiệu không được để trống")
    @Size(max = 100, message = "Tên thương hiệu tối đa 100 ký tự")
    private String name;

    @Size(max = 120, message = "Slug tối đa 120 ký tự")
    private String slug;

    @JsonProperty("logo_url")
    @Size(max = 6990544, message = "Image must be at most 5 MB")
    private String logoUrl;

    @JsonProperty("is_active")
    @Builder.Default
    private Boolean isActive = true;
}
