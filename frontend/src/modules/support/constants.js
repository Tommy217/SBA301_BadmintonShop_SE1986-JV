export const ticketTypes = {
  REFUND: 'Trả hàng & Hoàn tiền',
  EXCHANGE: 'Đổi sản phẩm',
};

export const ticketStatuses = {
  PENDING: { label: 'Chờ tiếp nhận', variant: 'warning' },
  REJECTED: { label: 'Bị từ chối', variant: 'danger' },
  AWAITING_RETURN: { label: 'Chờ gửi lại hàng', variant: 'info' },
  INSPECTION_FAILED: { label: 'Kiểm tra không đạt', variant: 'danger' },
  PENDING_APPROVAL: { label: 'Chờ Quản lý duyệt', variant: 'primary' },
  PENDING_TRANSFER: { label: 'Chờ chuyển tiền', variant: 'warning' },
  REFUNDED: { label: 'Đã hoàn tiền', variant: 'success' },
  COMPLETED: { label: 'Đã hoàn tất đổi hàng', variant: 'success' },
  CANCELLED: { label: 'Đã hủy', variant: 'secondary' },
};

export const ticketCauses = {
  STORE_FAULT: {
    label: 'Lỗi do cửa hàng / Nhà sản xuất',
    desc: 'Lỗi sản phẩm, giao nhầm mã, thiếu phụ kiện... Cửa hàng chịu hoàn phí giao hàng.',
  },
  CUSTOMER_PREFERENCE: {
    label: 'Nhu cầu cá nhân của khách hàng',
    desc: 'Đổi ý, không vừa kích thước, muốn đổi màu... Khách tự chịu phí vận chuyển hoàn hàng.',
  },
};

export const inspectionResults = {
  PASSED: 'Đạt yêu cầu (Sản phẩm còn nguyên trạng/đúng điều kiện)',
  FAILED: 'Không đạt (Sản phẩm bị can thiệp, hư hại do sử dụng, sai quy cách)',
};

export const APPROVAL_THRESHOLD = 3000000; // 3,000,000 VND

export const RETURN_WINDOW_DAYS = 7; // 7 days from confirmed delivery
