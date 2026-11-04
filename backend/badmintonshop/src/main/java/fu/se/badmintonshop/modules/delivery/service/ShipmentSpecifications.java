package fu.se.badmintonshop.modules.delivery.service;

import fu.se.badmintonshop.modules.delivery.entity.Carrier;
import fu.se.badmintonshop.modules.delivery.entity.Shipment;
import fu.se.badmintonshop.modules.order.entity.Order;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// bo loc danh sach van don cho manager: status, carrier, q (ma van don / ma don / nguoi nhan / sdt)
public final class ShipmentSpecifications {

    private ShipmentSpecifications() {
    }

    public static Specification<Shipment> filter(String status, String carrierCode, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim().toLowerCase(Locale.ROOT)));
            }

            if (hasText(carrierCode)) {
                Join<Shipment, Carrier> carrier = root.join("carrier", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(carrier.<String>get("code")), carrierCode.trim().toLowerCase(Locale.ROOT)));
            }

            if (hasText(q)) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Shipment, Order> order = root.join("order", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("trackingCode")), like),
                        cb.like(cb.lower(order.<String>get("orderCode")), like),
                        cb.like(cb.lower(order.<String>get("receiverName")), like),
                        cb.like(order.<String>get("receiverPhone"), like)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
