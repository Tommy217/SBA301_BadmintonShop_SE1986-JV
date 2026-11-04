package fu.se.badmintonshop.modules.delivery.gateway;

import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// chua goi doi tac that: chi ghi log, van don dung o trang thai requested
@Slf4j
@Component
public class NoopCarrierGateway implements CarrierGateway {

    @Override
    public void requestShipment(Shipment shipment) {
        log.info("[delivery] (noop) gui yeu cau van don #{} cho don hang #{}",
                shipment.getId(), shipment.getOrder().getId());
    }

    @Override
    public void cancelShipment(Shipment shipment, String reason) {
        log.info("[delivery] (noop) huy van don #{}: {}", shipment.getId(), reason);
    }
}
