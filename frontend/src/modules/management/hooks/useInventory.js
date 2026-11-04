import { useCallback, useEffect, useRef, useState } from 'react';
import { inventoryApi } from '@/modules/management/api/inventory.api';

export default function useInventory() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const sequence = useRef(0);
  const executing = useRef(false);

  const refresh = useCallback(async () => {
    const current = ++sequence.current;
    try {
      const result = await inventoryApi.list();
      if (current === sequence.current) {
        setData(result);
        setError('');
      }
      return result;
    } catch (failure) {
      if (current === sequence.current) setError(failure.message);
      throw failure;
    }
  }, []);

  useEffect(() => {
    refresh().catch(() => {});
    return () => {
      sequence.current += 1;
    };
  }, [refresh]);

  const act = useCallback(
    async (request) => {
      if (executing.current) throw new Error('Đang xử lý thao tác kho. Vui lòng đợi.');
      executing.current = true;
      setBusy(true);
      setError('');
      try {
        const result = await request();
        // A failed refresh must not make the user repeat an already committed receipt.
        await refresh().catch(() => {
          setError('Đã lưu thành công nhưng chưa tải lại được dữ liệu. Bấm Làm mới để cập nhật.');
        });
        return result;
      } finally {
        executing.current = false;
        setBusy(false);
      }
    },
    [refresh],
  );

  return { data, error, busy, act, refresh };
}
