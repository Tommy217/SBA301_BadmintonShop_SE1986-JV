export const paymentMethods = [
  {
    id: 'mbbank',
    name: 'VietQR qua payOS',
    description: 'Quét mã bằng ứng dụng ngân hàng hỗ trợ VietQR',
    mark: 'QR',
  },
];

export const paymentMethodNames = {
  mbbank: 'MB Bank',
  payos: 'VietQR qua payOS',
  momo: 'Ví MoMo',
  card: 'Thanh toán online',
};

export const transactionStatuses = {
  success: {
    title: 'Thanh toán thành công',
    description: 'Giao dịch đã được xác nhận. Đơn hàng của bạn đang được xử lý.',
    className: 'payment-result-success',
    icon: '✓',
  },
  failed: {
    title: 'Thanh toán thất bại',
    description: 'Giao dịch chưa hoàn tất. Bạn có thể thử thanh toán lại.',
    className: 'payment-result-failed',
    icon: '!',
  },
  cancelled: {
    title: 'Giao dịch đã bị hủy',
    description: 'Phiên thanh toán đã được hủy theo yêu cầu của bạn.',
    className: 'payment-result-failed',
    icon: '✕',
  },
  pending: {
    title: 'Đang chờ thanh toán',
    description: 'Giao dịch chưa được xác nhận. Vui lòng kiểm tra lại sau.',
    className: 'payment-result-pending',
    icon: '…',
  },
};
