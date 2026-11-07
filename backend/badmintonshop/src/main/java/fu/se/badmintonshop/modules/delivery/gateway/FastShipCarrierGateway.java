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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static fu.se.badmintonshop.modules.delivery.gateway.FastShipPayloads.text;

// goi FastShip (partner-system :8081, MongoDB): gui yeu cau -> doi tac duyet -> shop dong bo
// xac thuc bang header X-Api-Key; khong thu ho (COD). Chieu nguoc lai (doi tac -> shop) la webhook, xem FastShipWebhookService
@Slf4j
@Component
public class FastShipCarrierGateway implements CarrierGateway {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${fastship.api-url:http://localhost:8081}")
    private String apiUrl;

    @Value("${fastship.api-key:dev-fastship-key}")
    private String apiKey;

    // Render goi Free ngu sau ~15 phut, lan goi dau co the mat ~1 phut
    @Value("${fastship.timeout-seconds:60}")
    private long timeoutSeconds;

    @Override
    public void requestShipment(Shipment shipment) {
        if (shipment.getCarrier() == null || !"fastship".equals(shipment.getCarrier().getCode())) {
            throw DeliveryException.badRequest("Hiện chỉ tích hợp đơn vị vận chuyển FastShip.");
        }
        Order order = shipment.getOrder();
        Map<String, Object> body = new LinkedHashMap<>();
        // khoa lien ket: gui lai cung shopShipmentId thi FastShip tra ve yeu cau cu, khong tao trung
        body.put("shopShipmentId", shipment.getId());
        body.put("shopOrderId", String.valueOf(order.getId()));
        body.put("orderCode", order.getOrderCode());
        body.put("receiverName", order.getReceiverName());
        body.put("receiverPhone", order.getReceiverPhone());
        body.put("shippingAddress", order.getShippingAddress());
        body.put("serviceType", shipment.getServiceType());
        body.put("packageWeightGram", shipment.getWeightGrams() == null ? 500 : shipment.getWeightGrams());
        body.put("note", shipment.getNote());

        JsonNode data = call("POST", "/api/v1/shipment-requests", body);
        String partnerRef = text(data, "id");
        if (partnerRef == null) {
            throw new DeliveryException(HttpStatus.BAD_GATEWAY, "FastShip không trả mã yêu cầu.");
        }
        shipment.setPartnerRef(partnerRef);
        shipment.setCarrierStatusText(text(data, "statusText"));
        JsonNode version = data.get("version");
        if (version != null && version.isNumber()) {
            shipment.setPartnerVersion(version.asLong());
        }
        // gui lai mot yeu cau da duoc duyet truoc do: nhan luon ma / trang thai hien tai
        if (text(data, "trackingCode") != null) {
            shipment.setTrackingCode(text(data, "trackingCode"));
            shipment.setTrackingUrl(text(data, "trackingUrl"));
        }
        ShipmentStatus.fromValue(text(data, "status"))
                .filter(ShipmentStatus.REQUESTED::canTransitionTo)
                .ifPresent(s -> shipment.setStatus(s.getValue()));
    }

    @Override
    public void cancelShipment(Shipment shipment, String reason) {
        call("POST", "/api/v1/shipments/" + encode(shipment.getPartnerRef()) + "/cancel", Map.of("reason", reason));
    }

    @Override
    public CarrierTracking track(Shipment shipment) {
        JsonNode data = call("GET", "/api/v1/shipments/" + encode(shipment.getPartnerRef()), null);
        return FastShipPayloads.toTracking(data, shipment.getPartnerRef());
    }

    private JsonNode call(String method, String path, Map<String, Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl.replaceAll("/+$", "") + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("X-Api-Key", apiKey);
            if (body == null) {
                builder.GET();
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code / 100 != 2) {
                String message = message(response.body());
                log.warn("[delivery] FastShip {} {} -> {} {}", method, path, code, message);
                // 409 (sai trang thai) / 404 tu doi tac: tra ve 409 cho shop; con lai coi nhu doi tac loi
                HttpStatus status = code == 409 || code == 404 ? HttpStatus.CONFLICT : HttpStatus.BAD_GATEWAY;
                throw new DeliveryException(status, "FastShip: " + (message != null ? message : "HTTP " + code));
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

    private String message(String body) {
        try {
            return text(mapper.readTree(body), "message");
        } catch (Exception e) {
            return null;
        }
    }

    private DeliveryException unreachable(Exception e) {
        log.warn("[delivery] khong goi duoc FastShip: {}", e.toString());
        return new DeliveryException(HttpStatus.BAD_GATEWAY, "Không kết nối được FastShip. Hãy bật partner-system.");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
