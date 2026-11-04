package fu.se.badmintonshop.modules.delivery.service;

import fu.se.badmintonshop.modules.delivery.dto.*;
import fu.se.badmintonshop.modules.delivery.entity.Carrier;
import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import fu.se.badmintonshop.modules.delivery.entity.ShipmentEvent;
import fu.se.badmintonshop.modules.delivery.enums.ShipmentStatus;
import fu.se.badmintonshop.modules.delivery.exception.DeliveryException;
import fu.se.badmintonshop.modules.delivery.gateway.CarrierGateway;
import fu.se.badmintonshop.modules.delivery.mapper.ShipmentMapper;
import fu.se.badmintonshop.modules.delivery.repository.CarrierRepository;
import fu.se.badmintonshop.modules.delivery.repository.ShipmentRepository;
import fu.se.badmintonshop.modules.order.entity.Order;
import fu.se.badmintonshop.modules.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    // chi tao van don khi don da xac nhan / dang chuan bi hang
    static final Set<String> SHIPPABLE_ORDER_STATUSES = Set.of("confirmed", "preparing");

    // van don o cac trang thai nay khong con hieu luc, duoc phep tao van don moi cho don
    static final List<String> INACTIVE_SHIPMENT_STATUSES = List.of(
            ShipmentStatus.CANCELLED.getValue(), ShipmentStatus.REJECTED.getValue());

    static final int MAX_PAGE_SIZE = 100;

    private final ShipmentRepository shipmentRepository;
    private final CarrierRepository carrierRepository;
    private final OrderRepository orderRepository;
    private final CarrierGateway carrierGateway;

    // ---------- doc (manager / staff) ----------

    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> list(String status, String carrierCode, String q, int page, int size) {
        if (status != null && !status.isBlank() && ShipmentStatus.fromValue(status.trim()).isEmpty()) {
            throw DeliveryException.badRequest("Trạng thái vận đơn không hợp lệ: " + status);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Shipment> result = shipmentRepository.findAll(ShipmentSpecifications.filter(status, carrierCode, q), pageable);
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

    // ---------- tao / huy ----------

    @Transactional
    public ShipmentResponse create(CreateShipmentRequest request, Long staffId) {
        Order order = orderRepository.findById(request.getOrderId())
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
                "Gửi tới " + carrier.getName() + ", chờ đối tác duyệt"));

        Shipment saved = shipmentRepository.save(shipment);
        // luu DB truoc roi moi goi doi tac (GD4: loi goi doi tac thi giu requested, cho gui lai)
        carrierGateway.requestShipment(saved);
        return ShipmentMapper.toResponse(saved, true);
    }

    @Transactional
    public ShipmentResponse cancel(Long id, CancelShipmentRequest request) {
        Shipment shipment = findDetail(id);
        ShipmentStatus current = currentStatus(shipment);

        if (!current.isCancellable()) {
            throw DeliveryException.conflict("Vận đơn đang ở trạng thái \"" + current.getLabel() + "\", không thể huỷ.");
        }

        String reason = request.getReason().trim();
        carrierGateway.cancelShipment(shipment, reason);

        shipment.setStatus(ShipmentStatus.CANCELLED.getValue());
        shipment.setCancelledAt(LocalDateTime.now());
        shipment.setCancelReason(reason);
        shipment.addEvent(event(ShipmentStatus.CANCELLED, "Shop huỷ vận đơn", reason));
        return ShipmentMapper.toResponse(shipmentRepository.save(shipment), true);
    }

    // ---------- khach hang ----------

    // van don moi nhat cua don; Optional.empty() khi don chua co van don
    @Transactional(readOnly = true)
    public Optional<ShipmentResponse> getForCustomer(Long orderId, Long userId) {
        if (userId == null) {
            throw new DeliveryException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem vận đơn.");
        }
        Order order = orderRepository.findById(orderId)
                .filter(o -> userId.equals(o.getUserId()))
                // khong phai don cua minh thi bao khong tim thay, tranh lo don nguoi khac
                .orElseThrow(() -> DeliveryException.notFound("Không tìm thấy đơn hàng #" + orderId));

        return shipmentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .findFirst()
                .map(ShipmentMapper::toCustomerResponse);
    }

    // ---------- helper ----------

    private Shipment findDetail(Long id) {
        return shipmentRepository.findWithDetailsById(id)
                .orElseThrow(() -> DeliveryException.notFound("Không tìm thấy vận đơn #" + id));
    }

    private ShipmentStatus currentStatus(Shipment shipment) {
        return ShipmentStatus.fromValue(shipment.getStatus())
                .orElseThrow(() -> DeliveryException.conflict("Vận đơn có trạng thái lạ: " + shipment.getStatus()));
    }

    private ShipmentEvent event(ShipmentStatus status, String title, String description) {
        return ShipmentEvent.builder()
                .status(status.getValue())
                .title(title)
                .description(description)
                .source("manual")
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
