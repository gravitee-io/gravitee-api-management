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

import type { CurrentUser, SocialIdentityProvider } from './auth.types';
import { managementApi } from '../../shared/api/api-client';
import { useBootstrapStore } from '../../shared/config/bootstrap.store';

const USER_PROVIDER_ID_SELECTED = 'user-provider-id-selected';

const oidcManagers: Record<string, UserManager> = {};

function getOrCreateUserManager(provider: SocialIdentityProvider): UserManager {
    if (oidcManagers[provider.id]) return oidcManagers[provider.id];

    const config = useBootstrapStore.getState().config!;
    const baseURL = `${config.managementBaseURL}/organizations/${config.organizationId}`;

    oidcManagers[provider.id] = new UserManager({
        authority: provider.authorizationEndpoint,
        client_id: provider.clientId,
        redirect_uri: `${(window.location.origin + window.location.pathname).replace(/\/$/, '')}`,
        post_logout_redirect_uri: `${(window.location.origin + window.location.pathname).replace(/\/$/, '') + '/login'}`,
        scope: (provider.scopes ?? []).join(provider.scopeDelimiter ?? ' '),
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
            authorization_endpoint: provider.authorizationEndpoint,
            introspection_endpoint: provider.tokenIntrospectionEndpoint,
            token_endpoint: `${baseURL}/auth/oauth2/${provider.id}`,
            end_session_endpoint:
                provider.userLogoutEndpoint ?? `${(window.location.origin + window.location.pathname).replace(/\/$/, '') + '/login'}`,
        },
    });

    return oidcManagers[provider.id];
}

function findProvider(providerId: string): SocialIdentityProvider | undefined {
    return useBootstrapStore.getState().config?.identityProviders.find(p => p.id === providerId);
}

interface AuthState {
    user: CurrentUser | null;
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
                if (get().initialized) return;
                set({ loading: true });

                try {
                    const providerIdSelected = localStorage.getItem(USER_PROVIDER_ID_SELECTED);
                    const isOAuthCallback = providerIdSelected && window.location.search.includes('code=');
                    const provider = isOAuthCallback ? findProvider(providerIdSelected) : undefined;

                    if (provider) {
                        const manager = getOrCreateUserManager(provider);
                        const oidcUser = await manager.signinRedirectCallback();
                        const user = await managementApi.get<CurrentUser>('/user');
                        const redirectUrl = (oidcUser.state as string) ?? '/';
                        set({ user, loading: false, initialized: true, oauthRedirectUrl: redirectUrl });
                        return;
                    }

                    const user = await managementApi.get<CurrentUser>('/user');
                    set({ user, loading: false, initialized: true });
                } catch {
                    set({ user: null, loading: false, initialized: true });
                }
            },

            login: async (username: string, password: string) => {
                localStorage.removeItem(USER_PROVIDER_ID_SELECTED);
                await managementApi.post<void>('/user/login', undefined, {
                    Authorization: `Basic ${btoa(`${username}:${password}`)}`,
                });
                const user = await managementApi.get<CurrentUser>('/user');
                set({ user });
            },

            loginWithProvider: async (providerId: string, redirectUrl: string) => {
                const provider = findProvider(providerId);
                if (!provider) throw new Error(`Identity provider ${providerId} not found`);

                localStorage.setItem(USER_PROVIDER_ID_SELECTED, providerId);
                const manager = getOrCreateUserManager(provider);
                await manager.signinRedirect({ state: redirectUrl });
            },

            logout: async () => {
                await managementApi.post<void>('/user/logout').catch(() => {});

                const providerIdSelected = localStorage.getItem(USER_PROVIDER_ID_SELECTED);
                if (providerIdSelected) {
                    const manager = oidcManagers[providerIdSelected];
                    if (manager) {
                        await manager.removeUser().catch(() => {});
                        await manager.clearStaleState().catch(() => {});
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
