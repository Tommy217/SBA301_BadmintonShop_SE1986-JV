const fastshipPortal =
  import.meta.env.VITE_FASTSHIP_PORTAL_URL ||
  (import.meta.env.DEV ? 'http://localhost:8082/track' : '');

export const carriers = {
  fastship: {
    code: 'fastship',
    name: 'FastShip Express (Đối Tác)',
    shortName: 'FastShip',
    badgeColor: '#1d4ed8',
    badgeBg: '#dbeafe',
    hotline: '1900 6868',
    website: fastshipPortal,
    trackingUrl: fastshipPortal
      ? (code) => `${fastshipPortal}?code=${encodeURIComponent(code)}`
      : null,
  },
  ghn: {
    code: 'ghn',
    name: 'Giao Hàng Nhanh (GHN)',
    shortName: 'GHN',
    badgeColor: '#ea580c',
    badgeBg: '#ffedd5',
    hotline: '1900 636677',
    website: 'https://ghn.vn',
    trackingUrl: (code) => `https://donhang.ghn.vn/?order_code=${encodeURIComponent(code)}`,
  },
  ghtk: {
    code: 'ghtk',
    name: 'Giao Hàng Tiết Kiệm (GHTK)',
    shortName: 'GHTK',
    badgeColor: '#059669',
    badgeBg: '#d1fae5',
    hotline: '1900 6092',
    website: 'https://giaohangtietkiem.vn',
    trackingUrl: (code) => `https://i.ghtk.vn/${encodeURIComponent(code)}`,
  },
  viettelpost: {
    code: 'viettelpost',
    name: 'Viettel Post',
    shortName: 'Viettel Post',
    badgeColor: '#dc2626',
    badgeBg: '#fee2e2',
    hotline: '1900 8095',
    website: 'https://viettelpost.com.vn',
    trackingUrl: (code) =>
      `https://viettelpost.com.vn/tra-cuu-hanh-trinh-don/?code=${encodeURIComponent(code)}`,
  },
};

export const deliveryStatuses = {
  requested: {
    label: 'Chờ đối tác duyệt',
    variant: 'light',
    description: 'Shop đã gửi yêu cầu tạo vận đơn, chờ đối tác vận chuyển duyệt',
    stepIndex: 0,
  },
  rejected: {
    label: 'Đối tác từ chối',
    variant: 'danger',
    description: 'Đối tác vận chuyển từ chối yêu cầu, shop cần tạo lại vận đơn',
    stepIndex: 0,
  },
  pending_pickup: {
    label: 'Chờ lấy hàng',
    variant: 'warning',
    description: 'Shop đã tạo vận đơn, chờ bên thứ 3 đến lấy hàng',
    stepIndex: 1,
  },
  in_transit: {
    label: 'Đang trung chuyển',
    variant: 'info',
    description: 'Kiện hàng đang được luân chuyển giữa các kho bãi ĐVVC',
    stepIndex: 2,
  },
  out_for_delivery: {
    label: 'Đang phát hàng',
    variant: 'primary',
    description: 'Bưu tá ĐVVC đang trên đường đi giao cho người nhận',
    stepIndex: 3,
  },
  delivered: {
    label: 'Giao thành công',
    variant: 'success',
    description: 'Khách hàng đã nhận kiện hàng',
    stepIndex: 4,
  },
  delivery_failed: {
    label: 'Giao không thành công',
    variant: 'danger',
    description: 'Bưu tá không liên lạc được hoặc khách hẹn lại ngày giao',
    stepIndex: 3,
  },
  returned: {
    label: 'Chuyển hoàn về shop',
    variant: 'secondary',
    description: 'Đơn giao không thành công quá số lần quy định, hoàn về shop',
    stepIndex: 4,
  },
  cancelled: {
    label: 'Đã hủy vận đơn',
    variant: 'secondary',
    description: 'Vận đơn bên thứ 3 đã bị hủy',
    stepIndex: 0,
  },
};

export const carrierServices = {
  standard: 'Giao Tiêu Chuẩn',
  fast: 'Giao Nhanh',
  express: 'Giao Hỏa Tốc',
};
