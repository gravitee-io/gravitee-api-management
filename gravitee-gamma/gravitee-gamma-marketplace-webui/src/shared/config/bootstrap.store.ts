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

import type { IdentityProvider, IdentityProvidersResponse, PortalConfiguration } from '../../api/types';

export interface BootstrapConfig {
    portalBaseURL: string;
    environmentId: string;
    organizationId: string;
    identityProviders: IdentityProvider[];
    localLoginEnabled: boolean;
    forceLoginEnabled: boolean;
}

interface BootstrapState {
    config: BootstrapConfig | null;
    loading: boolean;
    error: Error | null;
    initialize: () => Promise<void>;
}

interface ConfigJson {
    baseURL: string;
    environmentId?: string;
}

interface BootstrapResponse {
    baseURL: string;
    environmentId: string;
    organizationId?: string;
}

function sanitizeBaseURL(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
}

function environmentIdFromConfig(config: ConfigJson): string | undefined {
    if (config.environmentId) {
        return config.environmentId;
    }
    const envIndex = config.baseURL.indexOf('/environments/');
    if (envIndex >= 0) {
        const envId = config.baseURL.substring(envIndex).split('/')[2];
        return envId || undefined;
    }
    return undefined;
}

function portalRootFromConfig(config: ConfigJson): string {
    let baseURL = sanitizeBaseURL(config.baseURL);
    const envIndex = baseURL.indexOf('/environments');
    if (envIndex >= 0) {
        baseURL = baseURL.substring(0, envIndex);
    }
    return baseURL;
}

async function fetchJson<T>(url: string): Promise<T> {
    const res = await fetch(url, { credentials: 'include', headers: { 'X-Requested-With': 'XMLHttpRequest' } });
    if (!res.ok) {
        throw new Error(`Failed to fetch ${url}: ${res.status}`);
    }
    return (await res.json()) as T;
}

async function fetchJsonOrUndefined<T>(url: string): Promise<T | undefined> {
    try {
        return await fetchJson<T>(url);
    } catch {
        return undefined;
    }
}

export const useBootstrapStore = create<BootstrapState>()(
    devtools(
        (set, get) => ({
            config: null,
            loading: false,
            error: null,

            initialize: async () => {
                if (get().config || get().loading) {
                    return;
                }
                set({ loading: true, error: null });

                try {
                    const configJson = await fetchJson<ConfigJson>('/assets/config.json');
                    const portalRoot = portalRootFromConfig(configJson);
                    const enforcedEnvironmentId = environmentIdFromConfig(configJson);
                    const bootstrapUrl = `${portalRoot}/ui/bootstrap${enforcedEnvironmentId ? `?environmentId=${enforcedEnvironmentId}` : ''}`;
                    const bootstrap = await fetchJson<BootstrapResponse>(bootstrapUrl);

                    const portalBaseURL = sanitizeBaseURL(bootstrap.baseURL);
                    const environmentId = bootstrap.environmentId;
                    const apiBase = `${portalBaseURL}/environments/${environmentId}`;

                    const [configuration, identities] = await Promise.all([
                        fetchJsonOrUndefined<PortalConfiguration>(`${apiBase}/configuration`),
                        fetchJsonOrUndefined<IdentityProvidersResponse>(`${apiBase}/configuration/identities`),
                    ]);

                    set({
                        config: {
                            portalBaseURL,
                            environmentId,
                            organizationId: bootstrap.organizationId ?? '',
                            identityProviders: identities?.data ?? [],
                            localLoginEnabled: configuration?.authentication?.localLogin?.enabled ?? true,
                            forceLoginEnabled: configuration?.authentication?.forceLogin?.enabled ?? false,
                        },
                        loading: false,
                    });
                } catch (error) {
                    set({ error: error instanceof Error ? error : new Error(String(error)), loading: false });
                    throw error;
                }
            },
        }),
        { name: 'bootstrap' },
    ),
);
