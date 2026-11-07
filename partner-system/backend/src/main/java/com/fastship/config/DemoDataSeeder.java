package com.fastship.config;

import com.fastship.document.PartnerShipment;
import com.fastship.document.TrackingEvent;
import com.fastship.domain.ShipmentStatus;
import com.fastship.repository.PartnerShipmentRepository;
import com.fastship.service.FeeCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// thay fastship_schema.sql cu: nap 2 van don mau cho trang tra cuu khi collection con trong
@Component
@ConditionalOnProperty(name = "fastship.seed-demo", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final PartnerShipmentRepository repository;
    private final String portal;

    public DemoDataSeeder(PartnerShipmentRepository repository,
                          @Value("${tracking.portal.url:http://localhost:8082/track}") String portal) {
        this.repository = repository;
        this.portal = portal;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(demo(-1L, "FS-882910", "Phạm Thành", "0326538343",
                "Số 12 ngõ 45, Đường Lê Lợi, Phường Bãi Cháy, TP. Hạ Long, Quảng Ninh", ShipmentStatus.IN_TRANSIT));
        repository.save(demo(-2L, "FS-449102", "Nguyễn Minh Anh", "0912345678",
                "25 Trần Duy Hưng, Cầu Giấy, Hà Nội", ShipmentStatus.DELIVERED));
    }

    private PartnerShipment demo(Long shopShipmentId, String code, String name, String phone, String address, ShipmentStatus status) {
        LocalDateTime created = LocalDateTime.now().minusDays(2);
        PartnerShipment s = new PartnerShipment();
        s.setShopCode("demo");
        s.setShopShipmentId(shopShipmentId);
        s.setShopOrderCode("DEMO" + code.substring(3));
        s.setTrackingCode(code);
        s.setTrackingUrl(portal + "?code=" + code);
        s.setReceiverName(name);
        s.setReceiverPhone(phone);
        s.setShippingAddress(address);
        s.setServiceType("standard");
        s.setServiceName(FeeCalculator.serviceName("standard"));
        s.setPackageWeightGram(650);
        s.setPackageDesc("1x Vợt Yonex Astrox 88D Play, 1x Bao vợt");
        s.setShippingFee(FeeCalculator.fee("standard", 650));
        s.setShipperName("Nguyễn Hoàng Nam");
        s.setShipperPhone("0987.654.321");
        s.setEstimatedDeliveryDate(created.plusDays(3));
        s.setCreatedAt(created);
        s.setApprovedAt(created);
        s.setShippedAt(created.plusHours(3));
        s.addEvent(event("pending_pickup", "FastShip duyệt và cấp mã vận đơn", "Bưu cục FastShip", created));
        s.addEvent(event("in_transit", "Bưu tá đã lấy hàng thành công", "Bưu cục Cầu Giấy, Hà Nội", created.plusHours(3)));
        if (status == ShipmentStatus.DELIVERED) {
            s.addEvent(event("out_for_delivery", "Bưu tá bắt đầu phát hàng", "Bưu cục phát gần nhất", created.plusDays(1)));
            s.addEvent(event("delivered", "Giao hàng thành công", address, created.plusDays(1).plusHours(4)));
            s.setDeliveredAt(created.plusDays(1).plusHours(4));
        }
        s.setStatus(status.getValue());
        s.setStatusText(status.getDefaultText());
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private TrackingEvent event(String status, String title, String location, LocalDateTime at) {
        TrackingEvent e = new TrackingEvent(status, title, location, null);
        e.setOccurredAt(at);
        return e;
    }
}
