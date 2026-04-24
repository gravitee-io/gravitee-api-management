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

import type { Environment } from './environment.types';
import { resolveEnvironmentFromSegment } from './environment.utils';
import { managementApi } from '../../shared/api/api-client';

interface EnvironmentState {
    organizationId: string;
    environmentId: string;
    environments: Environment[];
    currentEnvironment: Environment | null;
    loading: boolean;
    error: Error | null;
    initialized: boolean;
    initialize: (organizationId: string) => Promise<void>;
    setCurrentEnvironment: (env: Environment) => void;
    resolveEnvironment: (envHridOrId: string) => Environment | null;
    reset: () => void;
}

const initialState = {
    organizationId: '',
    environmentId: '',
    environments: [] as Environment[],
    currentEnvironment: null as Environment | null,
    loading: false,
    error: null as Error | null,
    initialized: false,
};

export const useEnvironmentStore = create<EnvironmentState>()(
    devtools(
        (set, get) => ({
            ...initialState,

            reset: () => set({ ...initialState }),

            resolveEnvironment: (envHridOrId: string) => resolveEnvironmentFromSegment(get().environments, envHridOrId),

            initialize: async (organizationId: string) => {
                if (get().initialized) return;
                set({ loading: true, error: null });

                try {
                    const environments = await managementApi.get<Environment[]>('/environments');
                    if (!environments?.length) {
                        set({
                            ...initialState,
                            organizationId,
                            initialized: true,
                            loading: false,
                            error: new Error('No environment found!'),
                        });
                        return;
                    }
                    const first = environments[0]!;
                    set({
                        organizationId,
                        environments,
                        currentEnvironment: first,
                        environmentId: first.id,
                        loading: false,
                        error: null,
                        initialized: true,
                    });
                } catch (e) {
                    set({
                        ...initialState,
                        organizationId,
                        loading: false,
                        error: e instanceof Error ? e : new Error(String(e)),
                        initialized: true,
                    });
                }
            },

            setCurrentEnvironment: (env: Environment) => {
                set({ currentEnvironment: env, environmentId: env.id, organizationId: env.organizationId || get().organizationId });
            },
        }),
        { name: 'environment' },
    ),
);
