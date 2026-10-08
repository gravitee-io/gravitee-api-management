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

import type { RemoteModuleLoadStatus } from './modules.remotes';
import type { GammaModule } from './modules.types';

interface ModulesState {
    modules: GammaModule[];
    /** The modules whose load the user has been told about, and how it is going. */
    moduleLoadStatuses: Record<string, RemoteModuleLoadStatus>;
    setModules: (modules: GammaModule[]) => void;
    setModuleLoadStatus: (moduleId: string, status: RemoteModuleLoadStatus | undefined) => void;
}

export const useModulesStore = create<ModulesState>(set => ({
    modules: [],
    moduleLoadStatuses: {},
    setModules: modules => set({ modules }),
    setModuleLoadStatus: (moduleId, status) =>
        set(state => {
            const moduleLoadStatuses = { ...state.moduleLoadStatuses };
            if (status) moduleLoadStatuses[moduleId] = status;
            else delete moduleLoadStatuses[moduleId];
            return { moduleLoadStatuses };
        }),
}));
