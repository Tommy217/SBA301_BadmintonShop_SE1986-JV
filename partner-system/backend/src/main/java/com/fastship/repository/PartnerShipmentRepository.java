package com.fastship.repository;

import com.fastship.document.PartnerShipment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerShipmentRepository extends MongoRepository<PartnerShipment, String> {

    Optional<PartnerShipment> findByShopCodeAndShopShipmentId(String shopCode, Long shopShipmentId);

    Optional<PartnerShipment> findByTrackingCode(String trackingCode);

    boolean existsByTrackingCode(String trackingCode);

    List<PartnerShipment> findAllByOrderByCreatedAtDesc();

    List<PartnerShipment> findByStatusOrderByCreatedAtDesc(String status);
}
