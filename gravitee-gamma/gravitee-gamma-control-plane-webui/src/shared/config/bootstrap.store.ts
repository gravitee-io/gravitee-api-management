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

import { isLocalLoginEnabled, type LocalLoginConsoleSettings } from '../../../../gamma-ui-shared/src/consoleSettings';
import type { SocialIdentityProvider } from '../../features/auth/auth.types';

export interface BootstrapConfig {
    managementBaseURL: string;
    gammaBaseURL: string;
    organizationId: string;
    identityProviders: SocialIdentityProvider[];
    localLoginEnabled: boolean;
}

interface BootstrapState {
    config: BootstrapConfig | null;
    loading: boolean;
    error: Error | null;
    loginMethodsFetchedAt: number | null;
    initialize: () => Promise<void>;
    refreshLoginMethods: () => Promise<void>;
}

/** Skip a LoginPage refetch when bootstrap just loaded the same login-method APIs. */
const LOGIN_METHODS_FRESH_MS = 30_000;

let latestLoginMethodsRefreshId = 0;

function sanitizeBaseURL(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
}

function organizationManagementUrl(managementBaseURL: string, organizationId: string): string {
    return `${sanitizeBaseURL(managementBaseURL)}/organizations/${organizationId}`;
}

function localLoginEnabledFrom(consoleJson: unknown): boolean {
    if (!consoleJson || typeof consoleJson !== 'object') {
        return true;
    }
    return isLocalLoginEnabled(consoleJson as LocalLoginConsoleSettings);
}

async function fetchIdentityProviders(managementBaseURL: string, organizationId: string): Promise<SocialIdentityProvider[] | undefined> {
    try {
        const idpRes = await fetch(`${organizationManagementUrl(managementBaseURL, organizationId)}/social-identities`);
        if (idpRes.ok) {
            return (await idpRes.json()) as SocialIdentityProvider[];
        }
    } catch {
        // Non-fatal: keep the previous list, or an empty list on first load.
    }
    return undefined;
}

async function fetchLocalLoginEnabled(managementBaseURL: string, organizationId: string): Promise<boolean | undefined> {
    try {
        const consoleRes = await fetch(`${organizationManagementUrl(managementBaseURL, organizationId)}/console`);
        if (consoleRes.ok) {
            return localLoginEnabledFrom(await consoleRes.json());
        }
    } catch {
        // Non-fatal: keep the previous value, or leave local login off until a successful read.
    }
    return undefined;
}

async function loadLoginMethods(
    managementBaseURL: string,
    organizationId: string,
): Promise<{ identityProviders: SocialIdentityProvider[] | undefined; localLoginEnabled: boolean | undefined }> {
    const [identityProviders, localLoginEnabled] = await Promise.all([
        fetchIdentityProviders(managementBaseURL, organizationId),
        fetchLocalLoginEnabled(managementBaseURL, organizationId),
    ]);
    return { identityProviders, localLoginEnabled };
}

function isLoginMethodsFresh(fetchedAt: number | null): boolean {
    return fetchedAt !== null && Date.now() - fetchedAt < LOGIN_METHODS_FRESH_MS;
}

export const useBootstrapStore = create<BootstrapState>()(
    devtools(
        (set, get) => ({
            config: null,
            loading: false,
            error: null,
            loginMethodsFetchedAt: null,

            initialize: async () => {
                if (get().config || get().loading) return;
                set({ loading: true, error: null });

                try {
                    const constantsRes = await fetch('/constants.json');
                    if (!constantsRes.ok) throw new Error(`Failed to fetch constants.json: ${constantsRes.status}`);
                    const constants = await constantsRes.json();
                    const gammaBaseURL = sanitizeBaseURL(constants.gammaBaseURL);

                    const bootstrapRes = await fetch(`${gammaBaseURL}/ui/bootstrap`);
                    if (!bootstrapRes.ok) throw new Error(`Failed to fetch bootstrap config: ${bootstrapRes.status}`);
                    const bootstrap = await bootstrapRes.json();

                    const managementBaseURL = sanitizeBaseURL(bootstrap.managementBaseURL);
                    const organizationId = bootstrap.organizationId as string;
                    const loginMethods = await loadLoginMethods(managementBaseURL, organizationId);
                    const loginMethodsFetchedAt =
                        loginMethods.identityProviders !== undefined && loginMethods.localLoginEnabled !== undefined ? Date.now() : null;

                    set({
                        config: {
                            managementBaseURL,
                            gammaBaseURL: sanitizeBaseURL(bootstrap.gammaBaseURL),
                            organizationId,
                            identityProviders: loginMethods.identityProviders ?? [],
                            localLoginEnabled: loginMethods.localLoginEnabled ?? false,
                        },
                        loginMethodsFetchedAt,
                        loading: false,
                    });
                } catch (error) {
                    set({ error: error instanceof Error ? error : new Error(String(error)), loading: false });
                    throw error;
                }
            },

            refreshLoginMethods: async () => {
                const config = get().config;
                if (!config) {
                    return;
                }
                if (isLoginMethodsFresh(get().loginMethodsFetchedAt)) {
                    return;
                }

                const requestId = ++latestLoginMethodsRefreshId;
                const [identityProviders, localLoginEnabled] = await Promise.all([
                    fetchIdentityProviders(config.managementBaseURL, config.organizationId),
                    fetchLocalLoginEnabled(config.managementBaseURL, config.organizationId),
                ]);
                if (requestId !== latestLoginMethodsRefreshId) {
                    return;
                }

                const current = get().config;
                if (!current) {
                    return;
                }

                set({
                    config: {
                        ...current,
                        identityProviders: identityProviders ?? current.identityProviders,
                        localLoginEnabled: localLoginEnabled ?? current.localLoginEnabled,
                    },
                    loginMethodsFetchedAt:
                        identityProviders !== undefined && localLoginEnabled !== undefined ? Date.now() : get().loginMethodsFetchedAt,
                });
            },
        }),
        { name: 'bootstrap' },
    ),
);
