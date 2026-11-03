package fu.se.badmintonshop.modules.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BrandResponse {
    private Integer id;
    private String name;
    private String slug;
    @JsonProperty("logo_url")
    private String logoUrl;
    @JsonProperty("is_active")
    private Boolean isActive;
}
