import { createContext, use, useCallback, useContext, useState, type ReactNode } from 'react';
import { type User, login as apiLogin, logout as apiLogout, fetchCurrentUser } from './auth.api';

interface AuthContextValue {
    user: User | null;
    login: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// Resolves to User | null — never rejects, so an unauthenticated user won't trigger the ErrorBoundary.
const initialUserPromise: Promise<User | null> = fetchCurrentUser().catch(() => null);

export function AuthProvider({ children }: { readonly children: ReactNode }) {
    const initialUser = use(initialUserPromise);
    const [user, setUser] = useState<User | null>(initialUser);

    const login = useCallback(async (username: string, password: string) => {
        await apiLogin(username, password);
        const loggedIn = await fetchCurrentUser();
        setUser(loggedIn);
    }, []);

    const logout = useCallback(async () => {
        await apiLogout();
        setUser(null);
    }, []);

    return <AuthContext value={{ user, login, logout }}>{children}</AuthContext>;
}

export function useAuth(): AuthContextValue {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used within AuthProvider');
    return ctx;
}
