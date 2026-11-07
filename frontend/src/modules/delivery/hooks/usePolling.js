import { useEffect } from 'react';

// Goi lai `refresh` dinh ky de trang tu cap nhat khi webhook FastShip doi trang thai van don.
// Bo qua khi tab dang an; lam moi ngay khi quay lai tab. `refresh` nen on dinh (vd. reload cua useAsync).
export default function usePolling(refresh, { interval = 10000, enabled = true } = {}) {
  useEffect(() => {
    if (!enabled) return undefined;
    const tick = () => {
      if (document.visibilityState !== 'hidden') refresh();
    };
    const timer = window.setInterval(tick, interval);
    window.addEventListener('focus', tick);
    document.addEventListener('visibilitychange', tick);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener('focus', tick);
      document.removeEventListener('visibilitychange', tick);
    };
  }, [refresh, interval, enabled]);
}
