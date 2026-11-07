import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import Loading from '@/shared/components/Loading';
import { ShopContext } from './ShopContext';
import { fetchOrderData, fetchShopData } from './shop.api';

const LOAD_ERROR =
  'Không tải được dữ liệu cửa hàng. Hãy kiểm tra backend Java, kết nối database và đăng nhập rồi thử lại.';

// Holds the store snapshot from Java. `act(request)` runs a module API call that
// returns a new snapshot and stores it.
export default function ShopProvider({ children }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const orderRevision = useRef(0);
  const mutationActive = useRef(false);

  const load = useCallback(async () => {
    try {
      setData(await fetchShopData());
      setError('');
    } catch (failure) {
      setError(failure.message || LOAD_ERROR);
    }
  }, []);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    fetchShopData({ signal: controller.signal })
      .then((result) => active && setData(result))
      .catch((failure) => active && setError(failure.message || LOAD_ERROR));
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  const act = useCallback(async (request) => {
    mutationActive.current = true;
    orderRevision.current += 1;
    setBusy(true);
    setError('');
    try {
      const result = await request();
      setData((previous) => ({ ...previous, ...result }));
      if (result.catalogRefreshFailed) {
        setError(
          'Đã lưu thành công nhưng chưa tải lại được danh mục/tồn kho. Hãy làm mới trang để cập nhật.',
        );
      }
      return result;
    } catch (e) {
      setError(e.message);
      throw e;
    } finally {
      mutationActive.current = false;
      setBusy(false);
    }
  }, []);

  const refresh = useCallback(() => act(() => fetchShopData()), [act]);

  const refreshOrders = useCallback(async () => {
    if (mutationActive.current) return;
    const revision = ++orderRevision.current;
    const snapshot = await fetchOrderData();
    if (revision === orderRevision.current && !mutationActive.current) {
      setData((previous) => previous ? { ...previous, ...snapshot } : previous);
    }
  }, []);

  const value = useMemo(() => ({ data, busy, act, refresh, refreshOrders }), [data, busy, act, refresh, refreshOrders]);

  return (
    <ShopContext.Provider value={value}>
      {error && (
        <Alert variant="danger" className="m-3">
          {error} {!data && <Button onClick={load}>Thử lại</Button>}
        </Alert>
      )}
      {data ? children : !error && <Loading text="Đang tải cửa hàng…" />}
    </ShopContext.Provider>
  );
}
