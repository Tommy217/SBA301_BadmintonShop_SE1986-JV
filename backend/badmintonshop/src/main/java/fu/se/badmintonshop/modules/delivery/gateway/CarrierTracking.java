package fu.se.badmintonshop.modules.delivery.gateway;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// trang thai van don doc tu doi tac (pull sync hoac webhook)
// trackingCode / trackingUrl / phi / ngay du kien chi co sau khi doi tac duyet
// version: so phien ban document phia doi tac, tang moi lan cap nhat; dung de bo qua webhook cu den muon
public record CarrierTracking(
        String trackingCode,
        String trackingUrl,
        String status,
        String statusText,
        BigDecimal shippingFee,
        LocalDateTime estimatedDeliveryDate,
        String shipperName,
        String shipperPhone,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        Long version,
        List<Event> events) {

    // externalId: id duy nhat cua moc hanh trinh phia doi tac, dung de chong ghi trung
    public record Event(String externalId, String status, String title, String description,
                        String location, LocalDateTime occurredAt) {
    }
}
