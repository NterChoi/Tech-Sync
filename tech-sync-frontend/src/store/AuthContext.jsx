import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { tokenStore } from '../api/axios';

const AuthContext = createContext(null);
const USER_KEY = 'user_info';

function readUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (tokenStore.getAccess() ? readUser() : null));

  const signIn = useCallback(({ accessToken, refreshToken, userId, name }) => {
    tokenStore.set({ accessToken, refreshToken });
    const next = { userId, name };
    localStorage.setItem(USER_KEY, JSON.stringify(next));
    setUser(next);
  }, []);

  const signOut = useCallback(() => {
    tokenStore.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, signIn, signOut }), [user, signIn, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
