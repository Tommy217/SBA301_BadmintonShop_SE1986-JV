package com.fastship.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// collection webhook_outbox: moi lan duyet / tu choi / doi trang thai ghi 1 ban ghi, job gui sang shop va retry khi loi
@Document("webhook_outbox")
@CompoundIndex(name = "idx_due", def = "{'state': 1, 'nextRetryAt': 1}")
public class WebhookOutbox {

    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;            // = id moc hanh trinh vua them (TrackingEvent.id)

    @Indexed
    private String shipmentId;         // partnerRef
    private Long shopShipmentId;
    private String trackingCode;
    private String status;             // trang thai van don luc phat sinh su kien

    private String payload;            // JSON da ky, gui nguyen van
    private String state = PENDING;
    private int attempts;
    private Instant nextRetryAt;
    private Instant lastAttemptAt;
    private Integer lastStatusCode;
    private String lastError;
    private Instant createdAt;
    private Instant sentAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
    public Long getShopShipmentId() { return shopShipmentId; }
    public void setShopShipmentId(Long shopShipmentId) { this.shopShipmentId = shopShipmentId; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public Integer getLastStatusCode() { return lastStatusCode; }
    public void setLastStatusCode(Integer lastStatusCode) { this.lastStatusCode = lastStatusCode; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
