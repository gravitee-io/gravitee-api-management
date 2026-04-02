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

interface EnvironmentState {
    organizationId: string;
    environmentId: string;
    setEnvironment: (org: string, env: string) => void;
}

export const useEnvironmentStore = create<EnvironmentState>()(
    devtools(
        set => ({
            organizationId: '',
            environmentId: 'DEFAULT',
            setEnvironment: (organizationId, environmentId) => set({ organizationId, environmentId }),
        }),
        { name: 'environment' },
    ),
);
