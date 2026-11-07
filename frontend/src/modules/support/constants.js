export const ticketTypes = {
  REFUND: 'Trả hàng & Hoàn tiền',
};

export const ticketStatuses = {
  PENDING: { label: 'Chờ tiếp nhận', variant: 'warning' },
  REJECTED: { label: 'Bị từ chối', variant: 'danger' },
  AWAITING_RETURN: { label: 'Chờ gửi lại hàng', variant: 'info' },
  INSPECTION_FAILED: { label: 'Kiểm tra không đạt', variant: 'danger' },
  PENDING_APPROVAL: { label: 'Chờ Quản lý duyệt', variant: 'primary' },
  PENDING_TRANSFER: { label: 'Chờ chuyển tiền', variant: 'warning' },
  REFUNDED: { label: 'Đã hoàn tiền', variant: 'success' },
  CANCELLED: { label: 'Đã hủy', variant: 'secondary' },
};

export const ticketCauses = {
  STORE_FAULT: {
    label: 'Lỗi do cửa hàng / Nhà sản xuất',
    desc: 'Lỗi sản phẩm, giao nhầm mã, thiếu phụ kiện... Cửa hàng tiếp nhận và hoàn tiền bao gồm cả phí vận chuyển.',
  },
  CUSTOMER_PREFERENCE: {
    label: 'Nhu cầu cá nhân của khách hàng',
    desc: 'Đổi ý, không thích... Cửa hàng từ chối hỗ trợ và không hoàn tiền nếu sản phẩm không có lỗi từ shop.',
  },
};

export const inspectionResults = {
  PASSED: 'Đạt yêu cầu (Sản phẩm đúng hiện trạng lỗi khai báo/đủ điều kiện tiếp nhận)',
  FAILED: 'Không đạt (Sản phẩm bị can thiệp, hư hại do người dùng, sai quy cách)',
};

export const APPROVAL_THRESHOLD = 3000000; // 3,000,000 VND

export const RETURN_WINDOW_DAYS = 7; // 7 ngày kể từ khi đơn hàng giao thành công
