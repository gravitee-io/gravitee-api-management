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

function sanitizeBaseURL(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
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
                    const constantsRes = await fetch('/constants.json');
                    if (!constantsRes.ok) throw new Error(`Failed to fetch constants.json: ${constantsRes.status}`);
                    const constants = await constantsRes.json();
                    const portalBaseURL = sanitizeBaseURL(constants.portalBaseURL);

                    const bootstrapRes = await fetch(`${portalBaseURL}/ui/bootstrap`);
                    if (!bootstrapRes.ok) throw new Error(`Failed to fetch bootstrap config: ${bootstrapRes.status}`);
                    const bootstrap = await bootstrapRes.json();

                    set({
                        config: {
                            baseURL: portalBaseURL,
                            organizationId: bootstrap.organizationId,
                            environmentId: bootstrap.environmentId,
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
