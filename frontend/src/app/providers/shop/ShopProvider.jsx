import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import Loading from '@/shared/components/Loading';
import { ShopContext } from './ShopContext';
import { fetchShopData } from './shop.api';

const LOAD_ERROR =
  'Không tải được dữ liệu cửa hàng. Hãy kiểm tra backend Java, kết nối database và đăng nhập rồi thử lại.';

// Holds the store snapshot from Java. `act(request)` runs a module API call that
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
