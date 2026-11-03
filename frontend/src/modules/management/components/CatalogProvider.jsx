import { useCallback, useEffect, useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import Loading from '@/shared/components/Loading';
import { catalogApi } from '../api/catalog.api';
import { CatalogContext } from './CatalogContext';

export default function CatalogProvider({ children }) {
  const [data, setData] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    catalogApi.list({ signal: controller.signal })
      .then((result) => { if (!controller.signal.aborted) { setData(result); setError(''); } })
      .catch((e) => { if (!controller.signal.aborted) setError(e.message); });
    return () => controller.abort();
  }, [attempt]);

  const act = useCallback(async (request) => {
    setBusy(true);
    try {
      const saved = await request();
      // Reload authoritative data, including automatic price history and generated IDs.
      try {
        const result = await catalogApi.list();
        setData(result);
        setError('');
        return { ...result, saved };
      } catch (e) {
        // The write succeeded: close the form so retry cannot accidentally create twice.
        setError('Đã lưu nhưng chưa tải lại được Catalog: ' + e.message);
        return { saved };
      }
    } finally {
      setBusy(false);
    }
  }, []);

  return (
    <CatalogContext.Provider value={{ data, act, busy }}>
      {error && <Alert variant="danger">{error}{' '}
        <Button disabled={busy} onClick={() => { setError(''); setAttempt((n) => n + 1); }}>Tải lại Catalog</Button>
      </Alert>}
      {data ? children : !error && <Loading text="Đang tải Catalog từ Java…" />}
    </CatalogContext.Provider>
  );
}
