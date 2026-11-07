package fu.se.badmintonshop.modules.delivery.gateway;

import fu.se.badmintonshop.modules.delivery.entity.Shipment;

// cong goi sang don vi van chuyen. Loi goi doi tac nem DeliveryException (502)
public interface CarrierGateway {

    // gui yeu cau tao van don; gan partnerRef, trackingCode, phi... vao shipment
    void requestShipment(Shipment shipment);

    // yeu cau doi tac huy van don
    void cancelShipment(Shipment shipment, String reason);

    // doc trang thai hien tai cua van don tu doi tac
    CarrierTracking track(Shipment shipment);
}
