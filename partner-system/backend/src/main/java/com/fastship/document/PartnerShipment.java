package com.fastship.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// collection partner_shipments: 1 document = 1 van don, hanh trinh nhung trong events[]
@Document("partner_shipments")
// khoa lien ket voi shop: gui lai cung shopShipmentId khong tao don trung
@CompoundIndex(name = "uq_shop_shipment", def = "{'shopCode': 1, 'shopShipmentId': 1}", unique = true)
public class PartnerShipment {

    @Id
    private String id;                 // partnerRef tra ve cho shop

    @Version
    private Long version;              // chong 2 nguoi cap nhat cung luc

    private String shopCode;
    private Long shopShipmentId;       // = shipments.id ben shop
    private String shopOrderId;
    private String shopOrderCode;

    @Indexed(unique = true, sparse = true)
    private String trackingCode;       // chi co sau khi duyet
    private String trackingUrl;

    @Indexed
    private String status;
    private String statusText;

    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private String packageDesc;
    private Integer packageWeightGram;
    private String shopNote;

    private String serviceType;
    private String serviceName;
    private BigDecimal shippingFee;

    private String shipperName;
    private String shipperPhone;
    private LocalDateTime estimatedDeliveryDate;

    private String rejectReason;
    private String cancelReason;

    private LocalDateTime approvedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TrackingEvent> events = new ArrayList<>();

    // moi nhat dung dau
    public void addEvent(TrackingEvent event) {
        events.add(0, event);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getShopCode() { return shopCode; }
    public void setShopCode(String shopCode) { this.shopCode = shopCode; }
    public Long getShopShipmentId() { return shopShipmentId; }
    public void setShopShipmentId(Long shopShipmentId) { this.shopShipmentId = shopShipmentId; }
    public String getShopOrderId() { return shopOrderId; }
    public void setShopOrderId(String shopOrderId) { this.shopOrderId = shopOrderId; }
    public String getShopOrderCode() { return shopOrderCode; }
    public void setShopOrderCode(String shopOrderCode) { this.shopOrderCode = shopOrderCode; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getPackageDesc() { return packageDesc; }
    public void setPackageDesc(String packageDesc) { this.packageDesc = packageDesc; }
    public Integer getPackageWeightGram() { return packageWeightGram; }
    public void setPackageWeightGram(Integer packageWeightGram) { this.packageWeightGram = packageWeightGram; }
    public String getShopNote() { return shopNote; }
    public void setShopNote(String shopNote) { this.shopNote = shopNote; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public String getShipperName() { return shipperName; }
    public void setShipperName(String shipperName) { this.shipperName = shipperName; }
    public String getShipperPhone() { return shipperPhone; }
    public void setShipperPhone(String shipperPhone) { this.shipperPhone = shipperPhone; }
    public LocalDateTime getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<TrackingEvent> getEvents() { return events; }
    public void setEvents(List<TrackingEvent> events) { this.events = events; }
}
