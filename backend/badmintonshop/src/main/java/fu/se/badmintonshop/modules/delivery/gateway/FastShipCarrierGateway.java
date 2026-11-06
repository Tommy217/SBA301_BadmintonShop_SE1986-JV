package fu.se.badmintonshop.modules.delivery.gateway;

import com.fasterxml.jackson.databind.*;
import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
public class FastShipCarrierGateway implements CarrierGateway {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Value("${fastship.api-url:http://localhost:8081}") private String apiUrl;

    @Override
    public void requestShipment(Shipment shipment) {
        if (shipment.getCarrier() == null || !"fastship".equals(shipment.getCarrier().getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hiện đã tích hợp vận chuyển FastShip.");
        }
        var order = shipment.getOrder();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("shopOrderId", order.getId().toString());
        body.put("orderCode", order.getOrderCode());
        body.put("receiverName", order.getReceiverName());
        body.put("receiverPhone", order.getReceiverPhone());
        body.put("shippingAddress", order.getShippingAddress());
        body.put("serviceType", shipment.getServiceType());
        body.put("packageWeightGram", shipment.getWeightGrams() == null ? 500 : shipment.getWeightGrams());
        body.put("shippingFee", order.getShippingFee());
        body.put("codAmount", "unpaid".equals(order.getPaymentStatus()) ? order.getTotalAmount() : 0);
        var data = call("POST", "/api/v1/shipments/create", body);
        shipment.setTrackingCode(data.path("tracking_code").asText());
        shipment.setPartnerRef(shipment.getTrackingCode());
        shipment.setTrackingUrl(data.path("tracking_url").asText(null));
        shipment.setStatus(data.path("status").asText("requested"));
        shipment.setCarrierStatusText(data.path("status_text").asText(null));
        shipment.setShippingFee(data.path("shipping_fee").decimalValue());
        shipment.setCodAmount("unpaid".equals(order.getPaymentStatus()) ? order.getTotalAmount() : java.math.BigDecimal.ZERO);
    }

    @Override
    public void cancelShipment(Shipment shipment, String reason) {
        call("POST", "/api/v1/shipments/" + shipment.getTrackingCode() + "/cancel", Map.of());
    }

    public JsonNode track(String code) { return call("GET", "/api/v1/shipments/track/" + code, null); }

    private JsonNode call(String method, String path, Map<String, Object> body) {
        try {
            var builder = HttpRequest.newBuilder(URI.create(apiUrl.replaceAll("/+$", "") + path)).timeout(Duration.ofSeconds(10));
            if (body == null) builder.GET();
            else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastShip không xử lý được yêu cầu. Vui lòng kiểm tra dịch vụ đối tác.");
            return mapper.readTree(response.body()).path("data");
        } catch (ResponseStatusException error) { throw error;
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không kết nối được backend FastShip. Hãy bật partner-system.");
        }
    }
}
