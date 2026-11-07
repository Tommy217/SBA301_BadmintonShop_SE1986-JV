package fu.se.badmintonshop.modules.delivery.gateway;

import java.time.LocalDateTime;
import java.util.List;

// trang thai van don doc tu doi tac (dung cho pull sync)
public record CarrierTracking(
        String trackingCode,
        String status,
        String statusText,
        String shipperName,
        String shipperPhone,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        List<Event> events) {

    // externalId: id duy nhat cua moc hanh trinh phia doi tac, dung de chong ghi trung
    public record Event(String externalId, String status, String title, String description,
                        String location, LocalDateTime occurredAt) {
    }
}
