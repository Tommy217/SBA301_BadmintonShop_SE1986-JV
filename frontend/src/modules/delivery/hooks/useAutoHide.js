import { useEffect } from 'react';

// An thong bao sau `delay` ms (mac dinh 3 giay). `setMessage` la setter cua useState nen on dinh giua cac lan render.
// Truyen '' (vd. khi la thong bao loi) de giu nguyen, khong tu an.
export default function useAutoHide(message, setMessage, delay = 3000) {
  useEffect(() => {
    if (!message) return undefined;
    const timer = window.setTimeout(() => setMessage(''), delay);
    return () => window.clearTimeout(timer);
  }, [message, setMessage, delay]);
}
