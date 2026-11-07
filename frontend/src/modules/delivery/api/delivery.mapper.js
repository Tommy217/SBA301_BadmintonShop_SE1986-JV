// Doi ShipmentResponse (camelCase) cua BE sang shape cac trang delivery dang dung,
// de giu nguyen giao dien hien tai.
export function toDelivery(s) {
  if (!s) return null;
  return {
    id: s.id,
    order_id: s.orderId,
    order_code: s.orderCode,
    receiver_name: s.receiverName,
    receiver_phone: s.receiverPhone,
    shipping_address: s.shippingAddress,
    carrier: s.carrierCode,
    carrier_name: s.carrierName,
    carrier_hotline: s.carrierHotline,
    partner_ref: s.partnerRef,
    tracking_number: s.trackingCode,
    tracking_url: s.trackingUrl,
    service_type: s.serviceType,
    status: s.status,
    status_label: s.statusLabel,
    carrier_status_text: s.carrierStatusText,
    cancellable: Boolean(s.cancellable),
    // chua gui duoc sang doi tac thi cho "Gui lai"
    resendable: s.status === 'requested' && s.partnerRef == null && s.cancellable != null,
    shipping_fee: s.shippingFee ?? 0,
    cod_amount: 0,
    weight_grams: s.weightGrams,
    note: s.note,
    shipper_name: s.shipperName,
    shipper_phone: s.shipperPhone,
    estimated_delivery_date: s.estimatedDeliveryDate,
    shipped_at: s.shippedAt,
    delivered_at: s.deliveredAt,
    cancelled_at: s.cancelledAt,
    cancel_reason: s.cancelReason,
    last_synced_at: s.lastSyncedAt,
    created_at: s.createdAt,
    updated_at: s.updatedAt,
    // moi nhat truoc, giong thu tu ShopService cu
    tracking_logs: [...(s.events || [])]
      .sort((a, b) => new Date(b.occurredAt || 0) - new Date(a.occurredAt || 0))
      .map((e) => ({
        id: e.id,
        status: e.status,
        title: e.title,
        note: e.description,
        location: e.location,
        source: e.source,
        timestamp: e.occurredAt,
      })),
  };
}

export const toDeliveries = (list) => (list || []).map(toDelivery);
