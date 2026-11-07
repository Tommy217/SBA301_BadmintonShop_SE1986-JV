package fu.se.badmintonshop.modules.delivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.gateway.FastShipPayloads;
import fu.se.badmintonshop.modules.delivery.gateway.FastShipSignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Nhan webhook FastShip: POST /api/webhooks/carriers/fastship.
 * Body: { eventId, type, occurredAt, shopShipmentId, partnerRef, status, version, data: {van don day du} }.
 * Header X-FastShip-Signature = "sha256=" + hex(HMAC-SHA256(fastship.webhook-secret, body goc)).
 */
@Slf4j
@Service
public class FastShipWebhookService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ShipmentService shipmentService;
    private final String secret;

    public FastShipWebhookService(ShipmentService shipmentService,
                                  @Value("${fastship.webhook-secret:dev-webhook-secret}") String secret) {
        this.shipmentService = shipmentService;
        this.secret = secret;
    }

    public ShipmentService.WebhookResult handle(byte[] body, String signature) {
        // 1. kiem chu ky tren dung byte nhan duoc, truoc khi doc JSON
        if (!FastShipSignature.verify(secret, body, signature)) {
            log.warn("[delivery] webhook FastShip sai chu ky");
            throw new DeliveryException(HttpStatus.UNAUTHORIZED, "Chữ ký webhook không hợp lệ.");
        }
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (IOException e) {
            throw DeliveryException.badRequest("Webhook không đúng định dạng JSON.");
        }
        long shipmentId = root.path("shopShipmentId").asLong(0);
        JsonNode data = root.path("data");
        if (shipmentId <= 0 || !data.isObject()) {
            throw DeliveryException.badRequest("Webhook thiếu shopShipmentId hoặc dữ liệu vận đơn.");
        }
        String partnerRef = FastShipPayloads.text(data, "id");
        String eventId = FastShipPayloads.text(root, "eventId");
        String eventKey = eventId == null || partnerRef == null ? null : FastShipPayloads.eventKey(partnerRef, eventId);

        ShipmentService.WebhookResult result = shipmentService.applyCarrierWebhook(
                shipmentId, partnerRef, eventKey, FastShipPayloads.toTracking(data, partnerRef));
        log.info("[delivery] webhook FastShip event {} van don #{} -> {}", eventId, shipmentId, result);
        return result;
    }
}
