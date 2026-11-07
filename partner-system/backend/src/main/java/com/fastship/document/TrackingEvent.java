package com.fastship.document;

import java.time.LocalDateTime;
import java.util.UUID;

// 1 moc hanh trinh, nhung trong document partner_shipments
public class TrackingEvent {
    private String id;
    private String status;
    private String title;
    private String location;
    private String description;
    private LocalDateTime occurredAt;

    public TrackingEvent() {
    }

    public TrackingEvent(String status, String title, String location, String description) {
        this.id = "evt-" + UUID.randomUUID().toString().substring(0, 8);
        this.status = status;
        this.title = title;
        this.location = location;
        this.description = description;
        this.occurredAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
