// Reads and writes the login session kept in localStorage.
const SESSION_KEY = 'user';

export function getSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null');
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

export function getToken(session = getSession()) {
  return session?.authToken || session?.token || null;
}

export function getRole(session = getSession()) {
  return session?.role ? session.role.toUpperCase() : null;
}

export const AUTH_EXPIRED_EVENT = 'auth-expired';
