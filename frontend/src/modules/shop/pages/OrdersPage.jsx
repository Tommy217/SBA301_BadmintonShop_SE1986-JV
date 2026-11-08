import { useState } from 'react';
import { Alert } from 'react-bootstrap';
import { Link, useLocation } from 'react-router';
import { useShop } from '@/app/providers/shop/ShopContext';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import Icon from '@/shared/components/Icon';
import { carriers, deliveryStatuses } from '@/modules/delivery/constants';
import { useMyShipments } from '@/modules/delivery/hooks/useDelivery';
import { payments } from '@/modules/management/constants';
import { paymentApi } from '@/modules/payment/api/payment.api';
import useOrderUpdates from '../hooks/useOrderUpdates';
import { getOrderStatus } from '../utils/orderStatus';
import { productImage, productImageFallback } from '../utils/productImages';
import ShopPagination from '../components/ShopPagination';
import '../orders.css';

const tabs = [
  { id: 'all', label: 'Tất cả', match: () => true },
  { id: 'unpaid', label: 'Chờ thanh toán', match: (o) => o.payment_status === 'unpaid' && !['cancelled', 'returned'].includes(o.status) },
  { id: 'processing', label: 'Đang xử lý', match: (o) => ['pending', 'confirmed', 'preparing'].includes(o.status) },
  { id: 'shipping', label: 'Đang giao', match: (o) => o.status === 'shipping' },
  { id: 'completed', label: 'Hoàn tất', match: (o) => o.status === 'completed' },
  { id: 'closed', label: 'Đã hủy / Trả hàng', match: (o) => ['cancelled', 'returned'].includes(o.status) },
];

export default function OrdersPage() {
  const { data, act, refresh } = useShop();
  const { state } = useLocation();
  const [activeTab, setActiveTab] = useState('all');
  const [page, setPage] = useState(1);
  const { data: shipments, reload: reloadShipments } = useMyShipments(data?.orders?.length > 0);
  useOrderUpdates(reloadShipments);
  const orders = [...data.orders].sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
  const visibleOrders = orders.filter(tabs.find((tab) => tab.id === activeTab).match);
  const totalPages = Math.max(1, Math.ceil(visibleOrders.length / 5));
  const currentPage = Math.min(page, totalPages);
  const pageOrders = visibleOrders.slice((currentPage - 1) * 5, currentPage * 5);

  const handleCancelPaymentOrder = async (orderId) => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy phiên thanh toán và hủy đơn hàng này không?')) return;
    try {
      await act(() => paymentApi.cancelPayment({ order_id: orderId, reason: 'Khách hàng chủ động hủy thanh toán' }));
    } catch (e) {
      alert(e.message || 'Không thể hủy thanh toán.');
    }
  };

  return <section className="customer-orders">
    <header className="orders-heading"><div><span className="orders-eyebrow">MUA SẮM CỦA BẠN</span><h1>Đơn hàng của tôi</h1><p>Theo dõi đơn hàng và những món đồ bạn yêu thích.</p></div><Link to="/products" className="orders-shop-link">Tiếp tục mua sắm <span aria-hidden="true">↗</span></Link></header>
    {state?.placed && <Alert variant="success">Đặt hàng thành công! Trạng thái mới nhất sẽ được cập nhật tại đây.</Alert>}
    <nav className="orders-tabs" aria-label="Lọc trạng thái đơn hàng">{tabs.map((tab) => <button type="button" key={tab.id} className={activeTab === tab.id ? 'is-active' : ''} aria-pressed={activeTab === tab.id} onClick={() => { setActiveTab(tab.id); setPage(1); }}>{tab.label}{tab.id === 'all' && <span>{orders.length}</span>}</button>)}</nav>
    <div className="orders-list" aria-live="polite">
      {!visibleOrders.length && <div className="orders-empty"><span className="orders-empty-icon"><Icon name="bag" size={32} /></span><h2>{orders.length ? 'Chưa có đơn hàng ở mục này' : 'Bạn chưa có đơn hàng nào'}</h2><p>Khám phá trang bị cho trận cầu tiếp theo của bạn.</p><Link className="order-action order-action-primary" to="/products">Khám phá sản phẩm</Link></div>}
      {pageOrders.map((order) => {
        const status = getOrderStatus(order.status);
        const closed = ['cancelled', 'returned'].includes(order.status);
        const delivery = (shipments || []).find((d) => String(d.order_id) === String(order.id));
        const carrier = carriers[delivery?.carrier];
        const deliveryStatus = deliveryStatuses[delivery?.status];
        const orderTickets = (data.support_tickets || []).filter((t) => String(t.order_id) === String(order.id));
        const activeTicket = orderTickets.find((t) => ['PENDING', 'AWAITING_RETURN', 'PENDING_APPROVAL', 'PENDING_TRANSFER'].includes(t.status));
        const latestTicket = [...orderTickets].sort((a, b) => new Date(b.created_at) - new Date(a.created_at))[0];
        const isRefunded = orderTickets.some((t) => t.status === 'REFUNDED');
        const hasInspectionFailed = orderTickets.some((t) => t.status === 'INSPECTION_FAILED' || t.inspection != null);
        const nonCancelledCount = orderTickets.filter((t) => t.status !== 'CANCELLED').length;
        const canCreateTicket = order.payment_status === 'paid' && !closed && !activeTicket && !isRefunded && !hasInspectionFailed && nonCancelledCount < 2;
        const payment = (data.payments || []).filter((p) => String(p.order_id) === String(order.id)).at(-1);
        const hasQrCode = Boolean(
          payment && (payment.qr_code_url || payment.qrCodeUrl || payment.checkout_url || payment.checkoutUrl || payment.gateway_txn_ref || payment.gatewayTxnRef)
        );
        const items = data.order_items.filter((item) => String(item.order_id) === String(order.id));
        const trackingUrl = '/orders/' + encodeURIComponent(order.id) + '/tracking';
        return <article className="order-tile" key={order.id}>
          <header className="order-tile-header"><div className="order-store"><Icon name="bag" size={18} /><strong>Badminton Shop</strong><span className="order-code">#{order.order_code}</span></div><span className="order-status" data-status={order.status}>{status.label}</span></header>
          {delivery && <Link to={trackingUrl} className="order-shipment"><Icon name="truck" size={17} /><span>{deliveryStatus?.label || delivery.status}{carrier && ' · ' + carrier.shortName}{delivery.tracking_number && <small>{delivery.tracking_number}</small>}</span><span aria-hidden="true">›</span></Link>}
          <div className="order-products">{items.map((item) => {
            const product = (data.products || []).find((p) => String(p.id) === String(item.product_id)) || { id: item.product_id, name: item.product_name };
            return <div className="order-product" key={item.id}>
              <Link to={trackingUrl} className="order-product-image" aria-label={'Xem đơn hàng của ' + item.product_name}><img src={productImage(data, product, item.variant_id)} onError={productImageFallback} alt={item.product_name} loading="lazy" /></Link>
              <div className="order-product-info"><h2>{item.product_name}</h2><span className="order-variant">{item.variant_info || 'Phân loại tiêu chuẩn'}</span><span className="order-product-quantity">Số lượng: {item.quantity}</span></div>
              <div className="order-product-price"><strong>{formatMoney(item.unit_price ?? item.line_total / item.quantity)}</strong><span>×{item.quantity}</span></div>
            </div>;
          })}</div>
          <div className="order-tile-footer">
            <div className="order-total-row"><span>{items.reduce((sum, item) => sum + Number(item.quantity), 0)} sản phẩm <span className="order-total-dot">·</span> Phí giao hàng {Number(order.shipping_fee) ? formatMoney(order.shipping_fee) : 'miễn phí'}</span><div>Thành tiền <strong>{formatMoney(order.total_amount)}</strong></div></div>
            <div className="order-bottom-row"><span className="order-payment-note"><span className={'order-payment-dot' + (order.payment_status === 'paid' ? ' is-paid' : '')} />{payments[order.payment_status] || order.payment_status}{payment?.method === 'cod' ? ' · Khi nhận hàng' : ' · VietQR payOS'}</span><div className="order-actions">
              {activeTicket ? (
                <Link className="order-action" to={'/support-tickets/' + encodeURIComponent(activeTicket.id)}>Xem phiếu hỗ trợ</Link>
              ) : canCreateTicket ? (
                <Link className="order-action" to={'/support-tickets/create?orderId=' + encodeURIComponent(order.id)}>
                  {latestTicket && latestTicket.status === 'REJECTED' ? 'Gửi lại khiếu nại' : 'Hỗ trợ / Đổi trả'}
                </Link>
              ) : latestTicket ? (
                <Link className="order-action" to={'/support-tickets/' + encodeURIComponent(latestTicket.id)}>Xem phiếu hỗ trợ</Link>
              ) : null}
              <Link className="order-action" to={trackingUrl}>Chi tiết đơn hàng</Link>
              {order.payment_status === 'unpaid' && !closed && payment?.method !== 'cod' && (
                <>
                  <Link
                    className="order-action"
                    to={'/payments/result?order=' + encodeURIComponent(order.id)}
                  >
                    Kiểm tra kết quả thanh toán
                  </Link>
                  <Link
                    className="order-action order-action-primary"
                    to={(hasQrCode ? '/payments/qr?order=' : '/payments?order=') + encodeURIComponent(order.id)}
                  >
                    Tiếp tục thanh toán
                  </Link>
                </>
              )}
            </div></div>
            <details className="order-address"><summary>Thông tin giao hàng <span>· {formatDateTime(order.created_at)}</span></summary><p><strong>{order.receiver_name}</strong> · {order.receiver_phone}<br />{order.shipping_address}</p>{order.note && <p>Ghi chú: {order.note}</p>}</details>
          </div>
        </article>;
      })}
    </div>
    <ShopPagination page={currentPage} totalPages={totalPages} total={visibleOrders.length} noun="đơn hàng" onChange={setPage} />
  </section>;
}
