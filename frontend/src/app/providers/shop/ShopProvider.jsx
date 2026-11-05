import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import Loading from '@/shared/components/Loading';
import { ShopContext } from './ShopContext';
import { fetchShopData } from './shop.api';

const LOAD_ERROR = 'Không tải được sản phẩm từ database. Hãy kiểm tra backend Java và kết nối database rồi thử lại.';

// Holds the store snapshot from the mock API. `act(request)` runs a module API call that
// returns a new snapshot and stores it.
export default function ShopProvider({ children }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setData(await fetchShopData());
      setError('');
    } catch {
      setError(LOAD_ERROR);
    }
  }, []);

  useEffect(() => {
    let active = true;
    fetchShopData()
      .then((result) => active && setData(result))
      .catch(() => active && setError(LOAD_ERROR));
    return () => {
      active = false;
    };
  }, []);

  const act = useCallback(async (request) => {
    setBusy(true);
    setError('');
    try {
      const result = await request();
      setData(result);
      return result;
    } catch (e) {
      setError(e.message);
      throw e;
    } finally {
      setBusy(false);
    }
  }, []);

  const refresh = useCallback(() => act(() => fetchShopData()), [act]);

  const value = useMemo(() => ({ data, busy, act, refresh }), [data, busy, act, refresh]);

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
