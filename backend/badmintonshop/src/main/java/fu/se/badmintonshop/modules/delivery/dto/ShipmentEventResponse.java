package fu.se.badmintonshop.modules.delivery.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentEventResponse {
    private Long id;
    private String status;
    private String title;
    private String description;
    private String location;
    // carrier | webhook | sync | manual
    private String source;
    private LocalDateTime occurredAt;
}
