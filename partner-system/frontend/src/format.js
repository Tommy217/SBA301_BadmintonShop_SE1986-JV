export function formatDate(iso) {
  if (!iso) return "Chưa cập nhật";
  const d = new Date(iso);
  if (isNaN(d.getTime())) return "Chưa cập nhật";
  return d.toLocaleString("vi-VN", { dateStyle: "medium", timeStyle: "short" });
}

export function formatMoney(amount) {
  return (Number(amount) || 0).toLocaleString("vi-VN") + " ₫";
}

export function getStatusBadge(status) {
  switch (status) {
    case "requested": return { label: "Chờ duyệt", cls: "badge-warning" };
    case "rejected": return { label: "Đã từ chối", cls: "badge-danger" };
    case "pending_pickup": return { label: "Chờ lấy hàng", cls: "badge-warning" };
    case "in_transit": return { label: "Đang trung chuyển", cls: "badge-info" };
    case "out_for_delivery": return { label: "Đang phát hàng", cls: "badge-primary" };
    case "delivered": return { label: "Giao thành công", cls: "badge-success" };
    case "delivery_failed": return { label: "Giao thất bại", cls: "badge-danger" };
    case "returned": return { label: "Đã chuyển hoàn", cls: "badge-danger" };
    case "cancelled": return { label: "Đã hủy vận đơn", cls: "badge-danger" };
    default: return { label: status || "Đang xử lý", cls: "badge-info" };
  }
}

// cac buoc buu ta duoc chuyen tiep, khop ShipmentStatus.allowedNext o backend
export const COURIER_NEXT = {
  pending_pickup: [["in_transit", "🚚 Lấy hàng & nhập kho"]],
  in_transit: [["out_for_delivery", "🛵 Bưu tá phát hàng"], ["delivery_failed", "⚠️ Giao thất bại"]],
  out_for_delivery: [["delivered", "✅ Giao thành công"], ["delivery_failed", "⚠️ Giao thất bại"]],
  delivery_failed: [["out_for_delivery", "🛵 Phát lại"], ["returned", "↩️ Chuyển hoàn về shop"]],
};
