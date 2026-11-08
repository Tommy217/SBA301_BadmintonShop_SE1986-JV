import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { paymentMethods } from '@/modules/payment/constants';
import { useShop } from '@/app/providers/shop/ShopContext';
import { paymentApi } from '../api/payment.api';

export default function usePayment(options = {}) {
  const shop = useShop();
  const [params] = useSearchParams();
  const orderId = params.get('order');
  const [snapshot, setSnapshot] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState(paymentMethods[0].id);
  const [isOrderExpanded, setIsOrderExpanded] = useState(true);
  const navigate = useNavigate();
  const lock = useRef(false);

  const fetchShopSnapshot = async () => {
    try {
      const data = await paymentApi.getShopData();
      setSnapshot({ orderId, data });
      return data;
    } catch (err) {
      if (err.name !== 'CanceledError') {
        setError('Không tải được đơn hàng. Hãy kiểm tra server và tải lại trang.');
      }
      return null;
    }
  };

  useEffect(() => {
    if (!orderId) return;
    let isMounted = true;

    fetchShopSnapshot();

    if (!options.enablePolling) {
      return () => {
        isMounted = false;
      };
    }

    const intervalId = setInterval(async () => {
      if (!isMounted) return;
      try {
        const data = await paymentApi.getShopData();
        if (!isMounted) return;
        setSnapshot({ orderId, data });

        const currentOrder = data?.orders?.find((o) => String(o.id) === String(orderId));
        const currentPayment = data?.payments?.filter((p) => String(p.order_id) === String(orderId)).at(-1);

        if (currentOrder?.payment_status === 'paid' || currentPayment?.status === 'paid') {
          clearInterval(intervalId);
          shop?.refresh?.();
          navigate('/payments/result?order=' + encodeURIComponent(orderId), { replace: true });
        } else if (
          currentOrder?.status === 'cancelled' ||
          currentPayment?.status === 'expired' ||
          currentPayment?.status === 'failed'
        ) {
          clearInterval(intervalId);
        }
      } catch {
        // silent polling failure
      }
    }, 3000);

    return () => {
      isMounted = false;
      clearInterval(intervalId);
    };
  }, [orderId, options.enablePolling, navigate]);
  const data = snapshot && String(snapshot.orderId) === String(orderId) ? snapshot.data : null;
  const order = data?.orders.find((o) => String(o.id) === String(orderId));
  const record = data?.payments.filter((p) => String(p.order_id) === String(orderId)).at(-1);
  const orderItems = (data?.order_items || [])
    .filter((i) => String(i.order_id) === String(orderId))
    .map((i) => ({
      id: i.id,
      name: i.product_name,
      detail: i.variant_info,
      quantity: i.quantity,
      price: i.unit_price,
    }));
  const available = !!order && !!record && record.method !== 'cod';
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
    if (lock.current || !available || order?.status === 'cancelled') return;
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
