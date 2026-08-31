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
import { UserManager, WebStorageStateStore } from 'oidc-client-ts';
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

import { portalApi } from '../../api/portal-client';
import type { IdentityProvider, User } from '../../api/types';
import { useBootstrapStore } from '../../shared/config/bootstrap.store';

const USER_PROVIDER_ID_SELECTED = 'user-provider-id-selected';

const oidcManagers: Record<string, UserManager> = {};

function getOrCreateUserManager(provider: IdentityProvider): UserManager {
    const existing = oidcManagers[provider.id];
    if (existing) {
        return existing;
    }

    const config = useBootstrapStore.getState().config;
    if (!config) {
        throw new Error('Bootstrap not initialized');
    }
    const baseURL = `${config.portalBaseURL}/environments/${config.environmentId}`;
    const origin = `${window.location.origin}${window.location.pathname}`.replace(/\/$/, '');

    oidcManagers[provider.id] = new UserManager({
        authority: provider.authorizationEndpoint ?? origin,
        client_id: provider.client_id ?? '',
        redirect_uri: origin,
        post_logout_redirect_uri: `${origin}/login`,
        scope: (provider.scopes ?? []).join(' '),
        response_type: 'code',
        response_mode: 'query',
        loadUserInfo: false,
        userStore: new WebStorageStateStore({ store: window.localStorage }),
        fetchRequestCredentials: 'include',
        extraHeaders: {
            'X-Requested-With': 'XMLHttpRequest',
            'X-Xsrf-Token': localStorage.getItem('XSRF-TOKEN') ?? '',
        },
        metadata: {
            authorization_endpoint: provider.authorizationEndpoint ?? '',
            introspection_endpoint: provider.tokenIntrospectionEndpoint,
            token_endpoint: `${baseURL}/auth/oauth2/${provider.id}`,
            end_session_endpoint: provider.userLogoutEndpoint ?? `${origin}/login`,
        },
    });

    return oidcManagers[provider.id];
}

function findProvider(providerId: string): IdentityProvider | undefined {
    return useBootstrapStore.getState().config?.identityProviders.find(p => p.id === providerId);
}

interface AuthState {
    user: User | null;
    loading: boolean;
    initialized: boolean;
    oauthRedirectUrl: string | null;
    initialize: () => Promise<void>;
    login: (username: string, password: string) => Promise<void>;
    loginWithProvider: (providerId: string, redirectUrl: string) => Promise<void>;
    logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
    devtools(
        (set, get) => ({
            user: null,
            loading: false,
            initialized: false,
            oauthRedirectUrl: null,

            initialize: async () => {
                if (get().initialized) {
                    return;
                }
                set({ loading: true });

                try {
                    const providerIdSelected = localStorage.getItem(USER_PROVIDER_ID_SELECTED);
                    const isOAuthCallback = providerIdSelected && window.location.search.includes('code=');
                    const provider = isOAuthCallback ? findProvider(providerIdSelected) : undefined;

                    if (provider) {
                        const manager = getOrCreateUserManager(provider);
                        const oidcUser = await manager.signinRedirectCallback();
                        const user = await portalApi.get<User>('/user');
                        const redirectUrl = (oidcUser.state as string) ?? '/';
                        set({ user, loading: false, initialized: true, oauthRedirectUrl: redirectUrl });
                        return;
                    }

                    const user = await portalApi.get<User>('/user');
                    set({ user, loading: false, initialized: true });
                } catch {
                    set({ user: null, loading: false, initialized: true });
                }
            },

            login: async (username: string, password: string) => {
                localStorage.removeItem(USER_PROVIDER_ID_SELECTED);
                await portalApi.post<void>('/auth/login', undefined, {
                    Authorization: `Basic ${btoa(`${username}:${password}`)}`,
                });
                const user = await portalApi.get<User>('/user');
                set({ user });
            },

            loginWithProvider: async (providerId: string, redirectUrl: string) => {
                const provider = findProvider(providerId);
                if (!provider) {
                    throw new Error(`Identity provider ${providerId} not found`);
                }

                localStorage.setItem(USER_PROVIDER_ID_SELECTED, providerId);
                const manager = getOrCreateUserManager(provider);
                await manager.signinRedirect({ state: redirectUrl });
            },

            logout: async () => {
                await portalApi.post<void>('/auth/logout').catch(() => undefined);

                const providerIdSelected = localStorage.getItem(USER_PROVIDER_ID_SELECTED);
                if (providerIdSelected) {
                    const manager = oidcManagers[providerIdSelected];
                    if (manager) {
                        await manager.removeUser().catch(() => undefined);
                        await manager.clearStaleState().catch(() => undefined);
                    }
                }

                localStorage.removeItem(USER_PROVIDER_ID_SELECTED);
                localStorage.removeItem('XSRF-TOKEN');
                set({ user: null });
            },
        }),
        { name: 'auth' },
    ),
);
