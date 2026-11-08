import { useCallback, useEffect, useMemo, useState } from 'react';
import { AUTH_EXPIRED_EVENT, clearSession, getToken, saveSession } from '@/shared/auth/session';
import { authApi } from '@/modules/auth/api/auth.api';
import { AuthContext } from './AuthContext';
import { Alert, Button } from 'react-bootstrap';
import Loading from '@/shared/components/Loading';
export default function AuthProvider({ children, onSessionExpired }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(() => Boolean(getToken()));
  const [connectionError, setConnectionError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const login = useCallback((session) => {
    saveSession(session);
    setUser(session);
    setConnectionError('');
  }, []);
  const clear = useCallback(() => {
    clearSession();
    setUser(null);
    setConnectionError('');
  }, []);
  const logout = useCallback(() => {
    const request = authApi.logout();
    clear();
    request.catch(() => {});
  }, [clear]);
  useEffect(() => {
    let active = true;
    const token = getToken();
    const identityRequest = token ? authApi.me() : Promise.resolve(null);
    identityRequest
      .then((identity) => {
        if (!active) return;
        if (identity) login({ ...identity, authToken: token });
        else {
          clear();
          if (token) onSessionExpired?.();
        }
      })
      .catch((error) => {
        if (!active) return;
        if ([401, 403, 404].includes(error.status)) {
          clear();
          onSessionExpired?.();
        } else
          setConnectionError(
            error.message || 'Chưa kiểm tra được phiên đăng nhập. Vui lòng thử lại.',
          );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    const expireSession = () => {
      clear();
      onSessionExpired?.();
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, expireSession);
    return () => {
      active = false;
      window.removeEventListener(AUTH_EXPIRED_EVENT, expireSession);
    };
  }, [login, clear, attempt, onSessionExpired]);
  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);
  return (
    <AuthContext.Provider value={value}>
      {loading ? (
        <Loading text="Đang xác thực phiên đăng nhập…" />
      ) : connectionError ? (
        <Alert variant="warning" className="m-3">
          {connectionError}{' '}
          <Button
            onClick={() => {
              setConnectionError('');
              setLoading(true);
              setAttempt((n) => n + 1);
            }}
          >
            Thử lại
          </Button>
        </Alert>
      ) : (
        children
      )}
    </AuthContext.Provider>
  );
}
