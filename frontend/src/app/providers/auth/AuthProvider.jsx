import { useCallback, useEffect, useMemo, useState } from 'react';
import { AUTH_EXPIRED_EVENT, clearSession, getToken, saveSession } from '@/shared/auth/session';
import { authApi } from '@/modules/auth/api/auth.api';
import { AuthContext } from './AuthContext';
export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(() => Boolean(getToken()));
  const login = useCallback((session) => {
    saveSession(session);
    setUser(session);
  }, []);
  const clear = useCallback(() => {
    clearSession();
    setUser(null);
  }, []);
  const logout = useCallback(() => {
    const request = authApi.logout();
    clear();
    request.catch(() => {});
  }, [clear]);
  useEffect(() => {
    let active = true;
    const token = getToken();
    if (token)
      authApi
        .me()
        .then((identity) => {
          if (active) login({ ...identity, authToken: token });
        })
        .catch(() => {
          if (active) clear();
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    else clearSession();
    window.addEventListener(AUTH_EXPIRED_EVENT, clear);
    return () => {
      active = false;
      window.removeEventListener(AUTH_EXPIRED_EVENT, clear);
    };
  }, [login, clear]);
  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);
  return <AuthContext.Provider value={value}>{loading ? null : children}</AuthContext.Provider>;
}
