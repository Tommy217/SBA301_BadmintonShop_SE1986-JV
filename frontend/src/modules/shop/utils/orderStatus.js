import { statuses } from '@/modules/management/constants';
const variants = { pending: 'warning', confirmed: 'info', preparing: 'primary', shipping: 'primary', completed: 'success', cancelled: 'danger', returned: 'secondary' };
const descriptions = {
  pending: 'Shop đang chờ xác nhận đơn hàng.',
  confirmed: 'Shop đã xác nhận đơn hàng và sẽ chuẩn bị hàng để giao.',
  preparing: 'Shop đang đóng gói và chuẩn bị bàn giao cho đối tác vận chuyển.',
  shipping: 'Đơn hàng đang được vận chuyển đến bạn.',
  completed: 'Đơn hàng đã hoàn tất.',
  cancelled: 'Đơn hàng đã được hủy.',
  returned: 'Đơn hàng đã được trả lại.',
};
export function getOrderStatus(status) {
  return { label: statuses[status] || status || 'Chưa xác định', variant: variants[status] || 'secondary', description: descriptions[status] || 'Đang cập nhật trạng thái đơn hàng.' };
}
