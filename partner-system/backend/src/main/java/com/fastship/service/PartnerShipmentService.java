package com.fastship.service;

import com.fastship.document.PartnerShipment;
import com.fastship.document.TrackingEvent;
import com.fastship.domain.ShipmentStatus;
import com.fastship.dto.ApproveRequest;
import com.fastship.dto.ShipmentRequestBody;
import com.fastship.dto.StatusUpdateRequest;
import com.fastship.exception.PartnerException;
import com.fastship.repository.PartnerShipmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PartnerShipmentService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DEFAULT_SHIPPER = "Nguyễn Văn Tuấn (FastShip)";
    private static final String DEFAULT_SHIPPER_PHONE = "0987.654.321";

    private final PartnerShipmentRepository repository;
    private final WebhookOutboxService outbox;
    private final String shopCode;
    private final String trackingPortalUrl;

    public PartnerShipmentService(PartnerShipmentRepository repository,
                                  WebhookOutboxService outbox,
                                  @Value("${fastship.shop-code:badminton-shop}") String shopCode,
                                  @Value("${tracking.portal.url:http://localhost:8082/track}") String trackingPortalUrl) {
        this.repository = repository;
        this.outbox = outbox;
        this.shopCode = shopCode;
        this.trackingPortalUrl = trackingPortalUrl;
    }

    // ---------- API cho shop ----------

    // tao yeu cau o trang thai requested; gui lai cung shopShipmentId thi tra ve don cu
    public PartnerShipment submit(ShipmentRequestBody body) {
        var existing = repository.findByShopCodeAndShopShipmentId(shopCode, body.shopShipmentId());
        if (existing.isPresent()) {
            return existing.get();
        }
        LocalDateTime now = LocalDateTime.now();
        PartnerShipment s = new PartnerShipment();
        s.setShopCode(shopCode);
        s.setShopShipmentId(body.shopShipmentId());
        s.setShopOrderId(body.shopOrderId());
        s.setShopOrderCode(body.orderCode());
        s.setReceiverName(body.receiverName().trim());
        s.setReceiverPhone(body.receiverPhone().trim());
        s.setShippingAddress(body.shippingAddress().trim());
        s.setServiceType(FeeCalculator.normalizeService(body.serviceType()));
        s.setServiceName(FeeCalculator.serviceName(body.serviceType()));
        s.setPackageWeightGram(body.packageWeightGram() == null ? 500 : body.packageWeightGram());
        s.setPackageDesc(body.packageDesc() == null || body.packageDesc().isBlank() ? "Dụng cụ cầu lông" : body.packageDesc());
        s.setShopNote(body.note());
        s.setShippingFee(FeeCalculator.fee(s.getServiceType(), s.getPackageWeightGram()));
        s.setStatus(ShipmentStatus.REQUESTED.getValue());
        s.setStatusText(ShipmentStatus.REQUESTED.getDefaultText());
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        s.addEvent(new TrackingEvent(s.getStatus(), "Nhận yêu cầu từ shop", "Cổng API FastShip",
                "Yêu cầu vận chuyển đơn " + (body.orderCode() == null ? "" : body.orderCode()) + " đang chờ duyệt."));
        try {
            return repository.save(s);
        } catch (DuplicateKeyException e) {
            // 2 request cung luc: request sau doc lai ban da luu
            return repository.findByShopCodeAndShopShipmentId(shopCode, body.shopShipmentId()).orElseThrow(() -> e);
        }
    }

    public PartnerShipment cancelByShop(String id, String reason) {
        PartnerShipment s = get(id);
        ShipmentStatus current = status(s);
        if (current == ShipmentStatus.CANCELLED) {
            return s;
        }
        if (!current.canTransitionTo(ShipmentStatus.CANCELLED)) {
            throw PartnerException.conflict("Vận đơn đang ở trạng thái \"" + current.getLabel() + "\", không thể huỷ.");
        }
        s.setCancelReason(reason);
        s.setCancelledAt(LocalDateTime.now());
        return move(s, ShipmentStatus.CANCELLED, "Shop huỷ vận đơn", "Hệ thống FastShip", reason);
    }

    public PartnerShipment get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> PartnerException.notFound("Không tìm thấy vận đơn " + id));
    }

    // ---------- man hinh nhan vien FastShip ----------

    public List<PartnerShipment> list(String status) {
        if (status == null || status.isBlank()) {
            return repository.findAllByOrderByCreatedAtDesc();
        }
        ShipmentStatus s = ShipmentStatus.fromValue(status)
                .orElseThrow(() -> PartnerException.badRequest("Trạng thái không hợp lệ: " + status));
        return repository.findByStatusOrderByCreatedAtDesc(s.getValue());
    }

    public PartnerShipment approve(String id, ApproveRequest req) {
        PartnerShipment s = get(id);
        requireTransition(s, ShipmentStatus.PENDING_PICKUP);
        LocalDateTime now = LocalDateTime.now();
        String code = newTrackingCode();
        int days = req != null && req.estimatedDays() != null ? req.estimatedDays() : FeeCalculator.estimatedDays(s.getServiceType());
        s.setTrackingCode(code);
        s.setTrackingUrl(trackingPortalUrl + "?code=" + code);
        if (req != null && req.shippingFee() != null) s.setShippingFee(req.shippingFee());
        s.setShipperName(req != null && notBlank(req.shipperName()) ? req.shipperName().trim() : DEFAULT_SHIPPER);
        s.setShipperPhone(req != null && notBlank(req.shipperPhone()) ? req.shipperPhone().trim() : DEFAULT_SHIPPER_PHONE);
        s.setEstimatedDeliveryDate(now.plusDays(days));
        s.setApprovedAt(now);
        return move(s, ShipmentStatus.PENDING_PICKUP, "FastShip duyệt và cấp mã vận đơn", "Bưu cục FastShip",
                "Mã vận đơn " + code + ". Đang điều phối bưu tá đến lấy hàng.");
    }

    public PartnerShipment reject(String id, String reason) {
        PartnerShipment s = get(id);
        requireTransition(s, ShipmentStatus.REJECTED);
        s.setRejectReason(reason);
        return move(s, ShipmentStatus.REJECTED, "FastShip từ chối yêu cầu", "Bưu cục FastShip", reason);
    }

    public PartnerShipment updateStatus(String id, StatusUpdateRequest req) {
        ShipmentStatus next = ShipmentStatus.fromValue(req.status())
                .filter(ShipmentStatus.COURIER_UPDATES::contains)
                .orElseThrow(() -> PartnerException.badRequest("Không cập nhật được sang trạng thái " + req.status() + "."));
        PartnerShipment s = get(id);
        requireTransition(s, next);
        LocalDateTime now = LocalDateTime.now();
        if (next == ShipmentStatus.IN_TRANSIT && s.getShippedAt() == null) s.setShippedAt(now);
        if (next == ShipmentStatus.DELIVERED) s.setDeliveredAt(now);
        String location = notBlank(req.location()) ? req.location().trim() : defaultLocation(next, s);
        return move(s, next, titleOf(next), location, notBlank(req.note()) ? req.note().trim() : null);
    }

    // ---------- tra cuu cong khai ----------

    public PartnerShipment track(String trackingCode) {
        return repository.findByTrackingCode(trackingCode.trim().toUpperCase())
                .orElseThrow(() -> PartnerException.notFound("Không tìm thấy bưu phẩm có mã vận đơn '" + trackingCode + "'."));
    }

    // ---------- helper ----------

    // moi lan doi trang thai (duyet, tu choi, buu ta cap nhat, shop huy) them 1 moc hanh trinh va 1 webhook bao shop
    private PartnerShipment move(PartnerShipment s, ShipmentStatus next, String title, String location, String note) {
        s.setStatus(next.getValue());
        s.setStatusText(note != null && next != ShipmentStatus.PENDING_PICKUP ? note : next.getDefaultText());
        s.setUpdatedAt(LocalDateTime.now());
        TrackingEvent event = new TrackingEvent(next.getValue(), title, location, note != null ? note : next.getDefaultText());
        s.addEvent(event);
        PartnerShipment saved = repository.save(s);
        outbox.enqueue(saved, event);
        return saved;
    }

    private void requireTransition(PartnerShipment s, ShipmentStatus next) {
        ShipmentStatus current = status(s);
        if (!current.canTransitionTo(next)) {
            throw PartnerException.conflict("Không thể chuyển từ \"" + current.getLabel() + "\" sang \"" + next.getLabel() + "\".");
        }
    }

    private ShipmentStatus status(PartnerShipment s) {
        return ShipmentStatus.fromValue(s.getStatus())
                .orElseThrow(() -> PartnerException.conflict("Vận đơn có trạng thái lạ: " + s.getStatus()));
    }

    private String newTrackingCode() {
        for (int i = 0; i < 20; i++) {
            String code = "FS-" + (100000 + RANDOM.nextInt(900000));
            if (!repository.existsByTrackingCode(code)) {
                return code;
            }
        }
        throw PartnerException.conflict("Không cấp được mã vận đơn, vui lòng thử lại.");
    }

    private static String titleOf(ShipmentStatus status) {
        return switch (status) {
            case IN_TRANSIT -> "Bưu tá đã lấy hàng thành công";
            case OUT_FOR_DELIVERY -> "Bưu tá bắt đầu phát hàng";
            case DELIVERED -> "Giao hàng thành công";
            case DELIVERY_FAILED -> "Giao hàng không thành công";
            case RETURNED -> "Đã chuyển hoàn về shop";
            default -> "Cập nhật hành trình";
        };
    }

    private static String defaultLocation(ShipmentStatus status, PartnerShipment s) {
        return switch (status) {
            case IN_TRANSIT -> "Kho trung chuyển FastShip";
            case OUT_FOR_DELIVERY -> "Bưu cục phát gần nhất";
            case DELIVERED, DELIVERY_FAILED -> s.getShippingAddress();
            case RETURNED -> "Kho Badminton Shop";
            default -> "Bưu cục FastShip";
        };
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }
}
