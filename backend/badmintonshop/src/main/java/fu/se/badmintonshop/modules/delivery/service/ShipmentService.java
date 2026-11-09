package fu.se.badmintonshop.modules.delivery.service;

import fu.se.badmintonshop.modules.delivery.dto.*;
import fu.se.badmintonshop.modules.delivery.entity.Carrier;
import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import fu.se.badmintonshop.modules.delivery.entity.ShipmentEvent;
import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.gateway.CarrierGateway;
import fu.se.badmintonshop.modules.delivery.gateway.CarrierTracking;
import fu.se.badmintonshop.modules.delivery.mapper.ShipmentMapper;
import fu.se.badmintonshop.modules.delivery.repository.CarrierRepository;
import fu.se.badmintonshop.modules.delivery.repository.ShipmentRepository;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    // chi tao van don khi don da xac nhan / dang chuan bi hang
    static final Set<String> SHIPPABLE_ORDER_STATUSES = Set.of("confirmed", "preparing");

    // van don o cac trang thai nay khong con hieu luc, duoc phep tao van don moi cho don
    static final List<String> INACTIVE_SHIPMENT_STATUSES = List.of(
            ShipmentStatus.CANCELLED.getValue(), ShipmentStatus.REJECTED.getValue());

    static final int MAX_PAGE_SIZE = 100;

    // ket qua xu ly 1 webhook. Truong hop nao cung tra 200 cho doi tac de doi tac khong gui lai mai
    public enum WebhookResult {
        APPLIED,    // da cap nhat van don
        DUPLICATE,  // moc hanh trinh nay da ghi (webhook gui lai, hoac pull sync da lay truoc)
        STALE,      // webhook cu den muon: shop da co phien ban moi hon
        FINAL       // van don da ket thuc o shop, khong doi nua
    }

    private final ShipmentRepository shipmentRepository;
    private final CarrierRepository carrierRepository;
    private final OrderRepository orderRepository;
    private final CarrierGateway carrierGateway;
    private final OrderFulfillmentService fulfillment;
    private final EntityManager entityManager;

    // ---------- doc (manager / staff) ----------

    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> list(String status, String carrierCode, String q, Long orderId, int page, int size) {
        if (status != null && !status.isBlank() && ShipmentStatus.fromValue(status.trim()).isEmpty()) {
            throw DeliveryException.badRequest("Trạng thái vận đơn không hợp lệ: " + status);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Shipment> result = shipmentRepository.findAll(
                ShipmentSpecifications.filter(status, carrierCode, q, orderId), pageable);
        return PageResponse.of(result, s -> ShipmentMapper.toResponse(s, false));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse get(Long id) {
        return ShipmentMapper.toResponse(findDetail(id), true);
    }

    @Transactional(readOnly = true)
    public List<CarrierResponse> activeCarriers() {
        return carrierRepository.findByIsActiveTrue().stream().map(ShipmentMapper::toCarrierResponse).toList();
    }

    // ---------- tao / gui lai / huy ----------

    @Transactional
    public ShipmentResponse create(CreateShipmentRequest request, Long staffId) {
        // khoa dong don hang: 2 request tao cung luc cho 1 don se chay lan luot
        Order order = Optional.ofNullable(entityManager.find(Order.class, request.getOrderId(), LockModeType.PESSIMISTIC_WRITE))
                .orElseThrow(() -> DeliveryException.notFound("Không tìm thấy đơn hàng #" + request.getOrderId()));

        if (!SHIPPABLE_ORDER_STATUSES.contains(order.getStatus())) {
            throw DeliveryException.conflict("Chỉ tạo vận đơn cho đơn hàng đã xác nhận hoặc đang chuẩn bị (hiện tại: " + order.getStatus() + ").");
        }
        if (shipmentRepository.existsByOrderIdAndStatusNotIn(order.getId(), INACTIVE_SHIPMENT_STATUSES)) {
            throw DeliveryException.conflict("Đơn hàng " + order.getOrderCode() + " đã có vận đơn còn hiệu lực.");
        }

        Carrier carrier = carrierRepository.findByCode(request.getCarrierCode().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> DeliveryException.badRequest("Không có đơn vị vận chuyển " + request.getCarrierCode()));
        if (!Boolean.TRUE.equals(carrier.getIsActive())) {
            throw DeliveryException.badRequest("Đơn vị vận chuyển " + carrier.getName() + " đang tạm ngưng.");
        }

        Shipment shipment = Shipment.builder()
                .order(order)
                .carrier(carrier)
                .serviceType(request.getServiceType() != null ? request.getServiceType() : "standard")
                .status(ShipmentStatus.REQUESTED.getValue())
                .weightGrams(request.getWeightGrams())
                .note(request.getNote())
                .createdBy(staffId)
                .build();
        shipment.addEvent(event(ShipmentStatus.REQUESTED, "Shop gửi yêu cầu tạo vận đơn",
                "Gửi tới " + carrier.getName(), "manual"));

        // luu DB truoc roi moi goi doi tac: goi loi van giu van don requested de "Gui lai"
        Shipment saved = shipmentRepository.save(shipment);

        //thangtd: call sang he thong doi tac
        submitToCarrier(saved, staffId);
        return ShipmentMapper.toResponse(shipmentRepository.save(saved), true);
    }

    // gui lai yeu cau cho doi tac khi lan truoc goi loi (van don requested, chua co partnerRef)
    @Transactional
    public ShipmentResponse resend(Long id, Long staffId) {
        Shipment shipment = findDetail(id);
        if (!ShipmentStatus.REQUESTED.getValue().equals(shipment.getStatus()) || shipment.getPartnerRef() != null) {
            throw DeliveryException.conflict("Vận đơn đã được đối tác tiếp nhận, không cần gửi lại.");
        }
        carrierGateway.requestShipment(shipment);
        afterCarrierAccepted(shipment, staffId);
        return ShipmentMapper.toResponse(shipmentRepository.save(shipment), true);
    }

    @Transactional
    public ShipmentResponse cancel(Long id, CancelShipmentRequest request, Long staffId) {
        Shipment shipment = findDetail(id);
        ShipmentStatus current = currentStatus(shipment);

        if (!current.isCancellable()) {
            throw DeliveryException.conflict("Vận đơn đang ở trạng thái \"" + current.getLabel() + "\", không thể huỷ.");
        }

        String reason = request.getReason().trim();
        // chua gui duoc sang doi tac thi chi huy o shop
        if (shipment.getPartnerRef() != null) {
            carrierGateway.cancelShipment(shipment, reason);
        }

        shipment.setStatus(ShipmentStatus.CANCELLED.getValue());
        shipment.setCancelledAt(LocalDateTime.now());
        shipment.setCancelReason(reason);
        shipment.addEvent(event(ShipmentStatus.CANCELLED, "Shop huỷ vận đơn", reason, "manual"));
        fulfillment.onShipmentStatus(shipment.getOrder(), ShipmentStatus.CANCELLED, staffId);
        return ShipmentMapper.toResponse(shipmentRepository.save(shipment), true);
    }

    // ---------- dong bo tu doi tac (pull sync: nut "Dong bo" + job dinh ky) ----------

    @Transactional
    public ShipmentResponse sync(Long id, Long actorId) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> DeliveryException.notFound("Không tìm thấy vận đơn #" + id));
        return ShipmentMapper.toResponse(pull(shipment, actorId), true);
    }

    @Transactional
    public ShipmentResponse syncForCustomer(Long orderId, Long userId) {
        Shipment shipment = latestOfOwnOrder(orderId, userId)
                .orElseThrow(() -> DeliveryException.notFound("Đơn hàng chưa có vận đơn."));
        return ShipmentMapper.toCustomerResponse(pull(shipment, userId));
    }

    // ---------- webhook tu doi tac (day) ----------

    /**
     * Ap ban chup van don doi tac gui qua webhook (chu ky da duoc kiem o FastShipWebhookService).
     * Thu tu: tim van don -> khop partnerRef -> bo qua moc trung -> bo qua phien ban cu -> bo qua van don da ket thuc
     * -> cap nhat van don + moc hanh trinh (source = webhook) + trang thai don hang, trong 1 transaction.
     *
     * @param eventKey id moc hanh trinh vua phat sinh (dang FS-partnerRef-eventId), null neu doi tac khong gui
     */
    @Transactional
    public WebhookResult applyCarrierWebhook(Long shipmentId, String partnerRef, String eventKey, CarrierTracking remote) {
        if (partnerRef == null) {
            throw DeliveryException.badRequest("Webhook thiếu mã vận đơn đối tác.");
        }
        Shipment shipment = findDetail(shipmentId);
        entityManager.lock(shipment, LockModeType.PESSIMISTIC_WRITE);

        if (shipment.getPartnerRef() == null) {
            // shop goi tao don bi timeout nhung doi tac da nhan: lay partnerRef tu webhook
            shipment.setPartnerRef(partnerRef);
        } else if (!shipment.getPartnerRef().equals(partnerRef)) {
            throw DeliveryException.conflict("Webhook không khớp vận đơn #" + shipmentId + ".");
        }
        if (eventKey != null && shipment.getEvents().stream().anyMatch(e -> eventKey.equals(e.getExternalEventId()))) {
            return WebhookResult.DUPLICATE;
        }
        if (remote.version() != null && shipment.getPartnerVersion() != null
                && remote.version() <= shipment.getPartnerVersion()) {
            return WebhookResult.STALE;
        }
        if (currentStatus(shipment).isFinal()) {
            return WebhookResult.FINAL;
        }
        apply(shipment, remote, "webhook", null);
        shipmentRepository.save(shipment);
        return WebhookResult.APPLIED;
    }

    // ---------- khach hang ----------

    // van don moi nhat cua don; Optional.empty() khi don chua co van don
    @Transactional(readOnly = true)
    public Optional<ShipmentResponse> getForCustomer(Long orderId, Long userId) {
        return latestOfOwnOrder(orderId, userId).map(ShipmentMapper::toCustomerResponse);
    }

    // van don cua tat ca don hang cua khach (trang "Don hang cua toi")
    @Transactional(readOnly = true)
    public List<ShipmentResponse> listForCustomer(Long userId) {
        requireLogin(userId);
        return shipmentRepository.findByOrderUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> {
                    ShipmentResponse r = ShipmentMapper.toResponse(s, false);
                    r.setNote(null);
                    r.setCreatedBy(null);
                    r.setPartnerRef(null);
                    r.setCancellable(null);
                    return r;
                })
                .toList();
    }

    // ---------- helper ----------

    private void submitToCarrier(Shipment shipment, Long actorId) {
        try {
            carrierGateway.requestShipment(shipment);
            afterCarrierAccepted(shipment, actorId);
        } catch (DeliveryException e) {
            if (e.getStatus() != HttpStatus.BAD_GATEWAY) {
                throw e;
            }
            log.warn("[delivery] van don #{} chua gui duoc sang doi tac: {}", shipment.getId(), e.getMessage());
            shipment.addEvent(event(ShipmentStatus.REQUESTED, "Chưa gửi được sang đối tác", e.getMessage(), "manual"));
        }
    }

    private void afterCarrierAccepted(Shipment shipment, Long actorId) {
        shipment.setLastSyncedAt(LocalDateTime.now());
        ShipmentStatus status = currentStatus(shipment);
        if (status != ShipmentStatus.REQUESTED) {
            shipment.addEvent(event(status, "Đối tác tiếp nhận vận đơn",
                    shipment.getTrackingCode() != null ? "Mã vận đơn " + shipment.getTrackingCode() : null, "carrier"));
        } else if (shipment.getPartnerRef() != null) {
            shipment.addEvent(event(status, "Đối tác đã nhận yêu cầu", "Chờ đối tác duyệt và cấp mã vận đơn", "carrier"));
        }
        fulfillment.onShipmentStatus(shipment.getOrder(), status, actorId);
    }

    private Shipment pull(Shipment shipment, Long actorId) {
        if (shipment.getPartnerRef() == null) {
            throw DeliveryException.conflict("Vận đơn chưa được gửi sang đối tác, hãy bấm \"Gửi lại\".");
        }
        entityManager.lock(shipment, LockModeType.PESSIMISTIC_WRITE);
        apply(shipment, carrierGateway.track(shipment), "sync", actorId);
        return shipmentRepository.save(shipment);
    }

    // chep trang thai doi tac vao van don shop; dung chung cho pull sync va webhook
    private void apply(Shipment shipment, CarrierTracking remote, String source, Long actorId) {
        if (remote.trackingCode() != null && shipment.getTrackingCode() != null
                && !remote.trackingCode().equals(shipment.getTrackingCode())) {
            throw new DeliveryException(HttpStatus.BAD_GATEWAY, "Mã vận đơn từ đối tác không khớp.");
        }
        ShipmentStatus next = ShipmentStatus.fromValue(remote.status())
                .orElseThrow(() -> new DeliveryException(HttpStatus.BAD_GATEWAY, "Đối tác trả trạng thái lạ: " + remote.status()));

        // doi tac vua duyet: nhan ma van don, link tra cuu, phi, ngay du kien
        if (remote.trackingCode() != null) shipment.setTrackingCode(remote.trackingCode());
        if (remote.trackingUrl() != null) shipment.setTrackingUrl(remote.trackingUrl());
        if (remote.shippingFee() != null) shipment.setShippingFee(remote.shippingFee());
        if (remote.estimatedDeliveryDate() != null) shipment.setEstimatedDeliveryDate(remote.estimatedDeliveryDate());
        ShipmentStatus current = currentStatus(shipment);

        // doi tac la nguon su that sau khi duyet: chap nhan nhay buoc (webhook truoc do bi mat), chi chan khi van don da ket thuc
        if (!current.isFinal() && next != current) {
            shipment.setStatus(next.getValue());
            if (next == ShipmentStatus.CANCELLED && shipment.getCancelledAt() == null) {
                shipment.setCancelledAt(LocalDateTime.now());
            }
            fulfillment.onShipmentStatus(shipment.getOrder(), next, actorId);
        }
        shipment.setCarrierStatusText(remote.statusText());
        shipment.setShipperName(remote.shipperName());
        shipment.setShipperPhone(remote.shipperPhone());
        if (remote.shippedAt() != null) shipment.setShippedAt(remote.shippedAt());
        if (remote.deliveredAt() != null) shipment.setDeliveredAt(remote.deliveredAt());
        if (remote.version() != null) {
            shipment.setPartnerVersion(Math.max(remote.version(),
                    shipment.getPartnerVersion() == null ? Long.MIN_VALUE : shipment.getPartnerVersion()));
        }
        shipment.setLastSyncedAt(LocalDateTime.now());

        Set<String> known = new HashSet<>();
        shipment.getEvents().forEach(e -> known.add(e.getExternalEventId()));
        for (CarrierTracking.Event e : remote.events()) {
            if (e.externalId() == null || !known.add(e.externalId())) {
                continue;
            }
            shipment.addEvent(ShipmentEvent.builder()
                    .externalEventId(e.externalId())
                    .status(e.status() != null ? e.status() : next.getValue())
                    .title(cut(e.title(), 150))
                    .description(cut(e.description(), 300))
                    .location(cut(e.location(), 200))
                    .source(source)
                    .occurredAt(e.occurredAt() != null ? e.occurredAt() : LocalDateTime.now())
                    .build());
        }
    }

    private Optional<Shipment> latestOfOwnOrder(Long orderId, Long userId) {
        requireLogin(userId);
        Order order = orderRepository.findById(orderId)
                .filter(o -> userId.equals(o.getUserId()))
                // khong phai don cua minh thi bao khong tim thay, tranh lo don nguoi khac
                .orElseThrow(() -> DeliveryException.notFound("Không tìm thấy đơn hàng #" + orderId));
        return shipmentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream().findFirst();
    }

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new DeliveryException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem vận đơn.");
        }
    }

    private Shipment findDetail(Long id) {
        return shipmentRepository.findWithDetailsById(id)
                .orElseThrow(() -> DeliveryException.notFound("Không tìm thấy vận đơn #" + id));
    }

    private ShipmentStatus currentStatus(Shipment shipment) {
        return ShipmentStatus.fromValue(shipment.getStatus())
                .orElseThrow(() -> DeliveryException.conflict("Vận đơn có trạng thái lạ: " + shipment.getStatus()));
    }

    private ShipmentEvent event(ShipmentStatus status, String title, String description, String source) {
        return ShipmentEvent.builder()
                .status(status.getValue())
                .title(title)
                .description(cut(description, 300))
                .source(source)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private static String cut(String text, int max) {
        return text == null ? null : text.substring(0, Math.min(text.length(), max));
    }
}
