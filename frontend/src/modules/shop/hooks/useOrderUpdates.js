import { useEffect } from 'react';
import { useShop } from '@/app/providers/shop/ShopContext';

// Keep customer pages current across staff/customer sessions without a busy overlay.
export default function useOrderUpdates(reloadShipments) {
  const { refreshOrders } = useShop();
  useEffect(() => {
    let active = true;
    let running = false;
    async function update() {
      if (!active || running || document.visibilityState === 'hidden') return;
      running = true;
      try {
        await refreshOrders();
        if (active) reloadShipments?.();
      } catch { /* Retain the last snapshot and retry on the next tick or focus. */ }
      finally { running = false; }
    }
    void update();
    const timer = window.setInterval(update, 10000);
    window.addEventListener('focus', update);
    document.addEventListener('visibilitychange', update);
    return () => {
      active = false;
      window.clearInterval(timer);
      window.removeEventListener('focus', update);
      document.removeEventListener('visibilitychange', update);
    };
  }, [refreshOrders, reloadShipments]);
}
