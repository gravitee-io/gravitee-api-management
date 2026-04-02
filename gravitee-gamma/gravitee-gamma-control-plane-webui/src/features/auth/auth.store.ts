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
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

import type { CurrentUser } from './auth.types';
import { managementApi } from '../../shared/api/api-client';

interface AuthState {
    user: CurrentUser | null;
    loading: boolean;
    initialized: boolean;
    initialize: () => Promise<void>;
    login: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
    devtools(
        (set, get) => ({
            user: null,
            loading: false,
            initialized: false,

            initialize: async () => {
                if (get().initialized) return;
                set({ loading: true });

                try {
                    const user = await managementApi.get<CurrentUser>('/user');
                    set({ user, loading: false, initialized: true });
                } catch {
                    set({ user: null, loading: false, initialized: true });
                }
            },

            login: async (username: string, password: string) => {
                await managementApi.post<void>('/user/login', undefined, {
                    Authorization: `Basic ${btoa(`${username}:${password}`)}`,
                });
                const user = await managementApi.get<CurrentUser>('/user');
                set({ user });
            },

            logout: async () => {
                await managementApi.post<void>('/user/logout').catch(() => {});
                localStorage.removeItem('XSRF-TOKEN');
                set({ user: null });
            },
        }),
        { name: 'auth' },
    ),
);
