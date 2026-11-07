package fu.se.badmintonshop.modules.delivery.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.order.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// goi API FastShip hien tai (partner-system :8081). GD3/GD4 se doi sang API co buoc duyet + X-Api-Key
// khong thu ho: codAmount luon = 0
@Slf4j
@Component
public class FastShipCarrierGateway implements CarrierGateway {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Value("${fastship.api-url:http://localhost:8081}")
    private String apiUrl;

    @Override
    public void requestShipment(Shipment shipment) {
        if (shipment.getCarrier() == null || !"fastship".equals(shipment.getCarrier().getCode())) {
            throw DeliveryException.badRequest("Hiện chỉ tích hợp đơn vị vận chuyển FastShip.");
        }
        Order order = shipment.getOrder();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("shopOrderId", String.valueOf(order.getId()));
        body.put("orderCode", order.getOrderCode());
        body.put("receiverName", order.getReceiverName());
        body.put("receiverPhone", order.getReceiverPhone());
        body.put("shippingAddress", order.getShippingAddress());
        body.put("serviceType", shipment.getServiceType());
        body.put("packageWeightGram", shipment.getWeightGrams() == null ? 500 : shipment.getWeightGrams());
        body.put("shippingFee", order.getShippingFee());
        body.put("codAmount", 0);

        JsonNode data = call("POST", "/api/v1/shipments/create", body);
        String trackingCode = text(data, "tracking_code");
        if (trackingCode == null) {
            throw new DeliveryException(HttpStatus.BAD_GATEWAY, "FastShip không trả mã vận đơn.");
        }
        shipment.setTrackingCode(trackingCode);
        // API hien tai dinh danh van don bang ma van don
        shipment.setPartnerRef(trackingCode);
        shipment.setTrackingUrl(text(data, "tracking_url"));
        shipment.setCarrierStatusText(text(data, "status_text"));
        if (data.hasNonNull("shipping_fee")) {
            shipment.setShippingFee(data.get("shipping_fee").decimalValue());
        }
        shipment.setCodAmount(BigDecimal.ZERO);
        String status = text(data, "status");
        ShipmentStatus.fromValue(status)
                .filter(ShipmentStatus.REQUESTED::canTransitionTo)
                .ifPresent(s -> shipment.setStatus(s.getValue()));
    }

    @Override
    public void cancelShipment(Shipment shipment, String reason) {
        call("POST", "/api/v1/shipments/" + shipment.getPartnerRef() + "/cancel", Map.of("reason", reason));
    }

    @Override
    public CarrierTracking track(Shipment shipment) {
        JsonNode data = call("GET", "/api/v1/shipments/track/" + shipment.getPartnerRef(), null);
        List<CarrierTracking.Event> events = new ArrayList<>();
        for (JsonNode e : data.path("events")) {
            events.add(new CarrierTracking.Event(
                    "FS-" + shipment.getPartnerRef() + "-" + e.path("id").asText(),
                    text(e, "status"), text(e, "title"), text(e, "description"), text(e, "location"),
                    time(e, "occurredAt")));
        }
        return new CarrierTracking(text(data, "trackingCode"), text(data, "status"), text(data, "statusText"),
                text(data, "shipperName"), text(data, "shipperPhone"),
                time(data, "shippedAt"), time(data, "deliveredAt"), events);
    }

    private JsonNode call(String method, String path, Map<String, Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl.replaceAll("/+$", "") + path))
                    .timeout(Duration.ofSeconds(10));
            if (body == null) {
                builder.GET();
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("[delivery] FastShip {} {} -> {}", method, path, response.statusCode());
                throw new DeliveryException(HttpStatus.BAD_GATEWAY, "FastShip không xử lý được yêu cầu (HTTP " + response.statusCode() + ").");
            }
            return mapper.readTree(response.body()).path("data");
        } catch (DeliveryException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unreachable(e);
        } catch (Exception e) {
            throw unreachable(e);
        }
    }

    private DeliveryException unreachable(Exception e) {
        log.warn("[delivery] khong goi duoc FastShip: {}", e.toString());
        return new DeliveryException(HttpStatus.BAD_GATEWAY, "Không kết nối được FastShip. Hãy bật partner-system.");
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    private static LocalDateTime time(JsonNode node, String field) {
        String v = text(node, field);
        return v == null ? null : LocalDateTime.parse(v.length() > 19 ? v.substring(0, 19) : v);
    }
}
