package fu.se.badmintonshop.modules.delivery.scheduler;

import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.repository.ShipmentRepository;
import fu.se.badmintonshop.modules.delivery.service.ShipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

// luoi an toan khi webhook bi mat: dinh ky pull sync cac van don chua ket thuc (mac dinh 5 phut/lan)
@Slf4j
@Component
public class ShipmentSyncJob {

    static final int BATCH_SIZE = 50;
    // doi tac loi lien tiep bang nay lan thi dung luot nay, tranh goi don dap khi doi tac sap
    static final int MAX_CONSECUTIVE_GATEWAY_ERRORS = 3;

    static final List<String> FINAL_STATUSES = Arrays.stream(ShipmentStatus.values())
            .filter(ShipmentStatus::isFinal).map(ShipmentStatus::getValue).toList();

    private final ShipmentRepository shipmentRepository;
    private final ShipmentService shipmentService;
    private final boolean enabled;

    public ShipmentSyncJob(ShipmentRepository shipmentRepository, ShipmentService shipmentService,
                           @Value("${fastship.sync-job.enabled:true}") boolean enabled) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentService = shipmentService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${fastship.sync-job.interval-ms:300000}",
            initialDelayString = "${fastship.sync-job.initial-delay-ms:60000}")
    public void run() {
        if (!enabled) {
            return;
        }
        // van don lau chua dong bo nhat duoc lam truoc
        List<Long> ids = shipmentRepository.findIdsForCarrierSync(FINAL_STATUSES, PageRequest.of(0, BATCH_SIZE));
        int synced = 0;
        int gatewayErrors = 0;
        for (Long id : ids) {
            try {
                shipmentService.sync(id, null);
                synced++;
                gatewayErrors = 0;
            } catch (DeliveryException e) {
                log.warn("[delivery] dong bo dinh ky van don #{} loi: {}", id, e.getMessage());
                if (e.getStatus() == HttpStatus.BAD_GATEWAY && ++gatewayErrors >= MAX_CONSECUTIVE_GATEWAY_ERRORS) {
                    log.warn("[delivery] FastShip loi lien tiep, dung luot dong bo nay");
                    break;
                }
            } catch (RuntimeException e) {
                log.warn("[delivery] dong bo dinh ky van don #{} loi: {}", id, e.toString());
            }
        }
        if (!ids.isEmpty()) {
            log.info("[delivery] dong bo dinh ky: {}/{} van don", synced, ids.size());
        }
    }
}
