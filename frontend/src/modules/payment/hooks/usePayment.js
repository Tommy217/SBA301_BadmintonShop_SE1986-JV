import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { paymentMethods } from '@/modules/payment/constants';
import { paymentApi } from '../api/payment.api';

export default function usePayment() {
  const [params] = useSearchParams();
  const orderId = params.get('order');
  const [snapshot, setSnapshot] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState(paymentMethods[0].id);
  const [isOrderExpanded, setIsOrderExpanded] = useState(true);
  const navigate = useNavigate();
  const lock = useRef(false);
  useEffect(() => {
    if (!orderId) return;
    const controller = new AbortController();
    paymentApi
      .getShopData({ signal: controller.signal })
      .then((data) => setSnapshot({ orderId, data }))
      .catch((err) => {
        if (err.name !== 'CanceledError')
          setError('Không tải được đơn hàng. Hãy kiểm tra server và tải lại trang.');
      });
    return () => controller.abort();
  }, [orderId]);
  const data = snapshot?.orderId === orderId ? snapshot.data : null;
  const order = data?.orders.find((o) => o.id === orderId);
  const record = data?.payments.filter((p) => p.order_id === orderId).at(-1);
  const orderItems = (data?.order_items || [])
    .filter((i) => i.order_id === orderId)
    .map((i) => ({
      id: i.id,
      name: i.product_name,
      detail: i.variant_info,
      quantity: i.quantity,
      price: i.unit_price,
    }));
  const available = !!order && !!record && record.method !== 'cod' && order.status !== 'cancelled';
  const payment = available
    ? {
        orderId,
        orderCode: order.order_code,
        total: order.total_amount,
        paymentMethod: record.gateway || paymentMethod,
        qrCodeUrl: record.qr_code_url,
        checkoutUrl: record.checkout_url,
      }
    : null;
  async function startPayment(event) {
    event.preventDefault();
    if (lock.current || !available) return;
    lock.current = true;
    setBusy(true);
    setError('');
    try {
      const result = await paymentApi.startPayment({
        order_id: orderId,
        gateway: paymentMethod,
      });
      setSnapshot({ orderId, data: result });
      navigate('/payments/qr?order=' + encodeURIComponent(orderId));
    } catch (err) {
      setError(err.message);
      // Allow the requested placeholder view when no payment provider is configured.
      // The backend still rejects payment creation and the order stays unpaid.
      if (err.status === 503) navigate('/payments/qr?order=' + encodeURIComponent(orderId));
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }
  return {
    orderId,
    order,
    payment,
    record,
    available,
    loading: !!orderId && !data && !error,
    error,
    busy,
    paymentMethod,
    setPaymentMethod,
    isOrderExpanded,
    setIsOrderExpanded,
    orderItems,
    subtotal: order?.subtotal || 0,
    shippingFee: order?.shipping_fee || 0,
    total: order?.total_amount || 0,
    startPayment,
  };
}
