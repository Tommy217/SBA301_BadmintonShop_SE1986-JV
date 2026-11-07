package com.fastship.repository;

import com.fastship.document.WebhookOutbox;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface WebhookOutboxRepository extends MongoRepository<WebhookOutbox, String> {

    // ban ghi den han gui (hoac den han gui lai), cu nhat truoc
    List<WebhookOutbox> findTop20ByStateAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(String state, Instant now);

    List<WebhookOutbox> findTop100ByOrderByCreatedAtDesc();

    List<WebhookOutbox> findTop100ByShipmentIdOrderByCreatedAtDesc(String shipmentId);

    List<WebhookOutbox> findTop100ByStateOrderByCreatedAtDesc(String state);
}
