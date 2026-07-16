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

import bundledConstants from '../../constants.json';

const APP_BASE_PATH = (bundledConstants.appBasePath ?? '/portal-editor').replace(/\/$/, '');

export interface BootstrapConfig {
    baseURL: string;
    organizationId: string;
    environmentId: string;
}

interface BootstrapState {
    config: BootstrapConfig | null;
    loading: boolean;
    error: Error | null;
    initialize: () => Promise<void>;
}

const DEFAULT_BOOTSTRAP_FALLBACK = {
    organizationId: 'local',
    environmentId: 'DEFAULT',
} as const;

function sanitizeBaseURL(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
}

function buildFallbackConfig(portalBaseURL: string): BootstrapConfig {
    return {
        baseURL: portalBaseURL,
        organizationId: DEFAULT_BOOTSTRAP_FALLBACK.organizationId,
        environmentId: DEFAULT_BOOTSTRAP_FALLBACK.environmentId,
    };
}

async function fetchPortalBaseURL(): Promise<string> {
    const bundledBaseURL = sanitizeBaseURL(bundledConstants.portalBaseURL ?? '/portal');

    try {
        const constantsRes = await fetch(`${APP_BASE_PATH}/constants.json`);
        if (!constantsRes.ok) {
            return bundledBaseURL;
        }

        const constants = await constantsRes.json();
        return sanitizeBaseURL(constants.portalBaseURL ?? bundledBaseURL);
    } catch {
        return bundledBaseURL;
    }
}

async function fetchRemoteBootstrap(portalBaseURL: string): Promise<BootstrapConfig | null> {
    try {
        const bootstrapRes = await fetch(`${portalBaseURL}/ui/bootstrap`);
        if (!bootstrapRes.ok) {
            return null;
        }

        const bootstrap = await bootstrapRes.json();
        return {
            baseURL: portalBaseURL,
            organizationId: bootstrap.organizationId,
            environmentId: bootstrap.environmentId,
        };
    } catch {
        return null;
    }
}

export const useBootstrapStore = create<BootstrapState>()(
    devtools(
        (set, get) => ({
            config: null,
            loading: false,
            error: null,

            initialize: async () => {
                if (get().config || get().loading) return;
                set({ loading: true, error: null });

                try {
                    const portalBaseURL = await fetchPortalBaseURL();
                    const remoteConfig = await fetchRemoteBootstrap(portalBaseURL);

                    if (remoteConfig) {
                        set({ config: remoteConfig, loading: false });
                        return;
                    }

                    console.warn(
                        `[portal-gamma] Bootstrap API unavailable at ${portalBaseURL}/ui/bootstrap — using local fallback config.`,
                    );
                    set({ config: buildFallbackConfig(portalBaseURL), loading: false });
                } catch (error) {
                    const resolvedError = error instanceof Error ? error : new Error(String(error));
                    const portalBaseURL = sanitizeBaseURL(bundledConstants.portalBaseURL ?? '/portal');
                    console.warn(
                        `[portal-gamma] Bootstrap initialization failed — using local fallback config.`,
                        resolvedError,
                    );
                    set({
                        config: buildFallbackConfig(portalBaseURL),
                        error: resolvedError,
                        loading: false,
                    });
                }
            },
        }),
        { name: 'bootstrap' },
    ),
);
