package fu.se.badmintonshop.modules.delivery.gateway;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// doc JSON van don cua FastShip, dung chung cho pull sync (GET /api/v1/shipments/{id}) va webhook
public final class FastShipPayloads {

    private FastShipPayloads() {
    }

    // id moc hanh trinh luu o shipment_events.external_event_id; pull sync va webhook cung 1 khoa nen khong ghi trung
    public static String eventKey(String partnerRef, String eventId) {
        return "FS-" + partnerRef + "-" + eventId;
    }

    public static CarrierTracking toTracking(JsonNode data, String partnerRef) {
        List<CarrierTracking.Event> events = new ArrayList<>();
        for (JsonNode e : data.path("events")) {
            String id = text(e, "id");
            events.add(new CarrierTracking.Event(
                    id == null ? null : eventKey(partnerRef, id),
                    text(e, "status"), text(e, "title"), text(e, "description"), text(e, "location"),
                    time(e, "occurredAt")));
        }
        JsonNode fee = data.get("shippingFee");
        JsonNode version = data.get("version");
        return new CarrierTracking(
                text(data, "trackingCode"), text(data, "trackingUrl"),
                text(data, "status"), statusText(data),
                fee != null && fee.isNumber() ? fee.decimalValue() : null,
                time(data, "estimatedDeliveryDate"),
                text(data, "shipperName"), text(data, "shipperPhone"),
                time(data, "shippedAt"), time(data, "deliveredAt"),
                version != null && version.isNumber() ? version.asLong() : null,
                events);
    }

    // bi tu choi thi dua ly do vao mo ta trang thai
    static String statusText(JsonNode data) {
        String reject = text(data, "rejectReason");
        return reject != null ? "FastShip từ chối: " + reject : text(data, "statusText");
    }

    public static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    // ISO-8601 tu Jackson phia doi tac; cat ve yyyy-MM-ddTHH:mm:ss
    static LocalDateTime time(JsonNode node, String field) {
        String v = text(node, field);
        if (v == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(v.length() > 19 ? v.substring(0, 19) : v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
