package fu.se.badmintonshop.modules.delivery.gateway;

import fu.se.badmintonshop.modules.delivery.entity.Shipment;

// cong goi sang don vi van chuyen. GD1 dung NoopCarrierGateway, GD4 thay bang FastShipCarrierGateway
public interface CarrierGateway {

    // gui yeu cau tao van don; ban that se gan partnerRef vao shipment
    void requestShipment(Shipment shipment);

    // yeu cau doi tac huy van don
    void cancelShipment(Shipment shipment, String reason);
}
