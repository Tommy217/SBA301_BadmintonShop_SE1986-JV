package fu.se.badmintonshop.modules.delivery.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrierResponse {
    private Integer id;
    private String code;
    private String name;
    private String hotline;
}
