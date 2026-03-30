/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
