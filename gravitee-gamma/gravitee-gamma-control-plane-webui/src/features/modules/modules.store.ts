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

/** `pending` until the list is read: until then, no module can be said to be missing. */
export type ModuleListStatus = 'pending' | 'loaded' | 'failed';

interface ModulesState {
    modules: GammaModule[];
    listStatus: ModuleListStatus;
    /** Counts the reads of the list asked for, so that a page outside the module routes can ask for one. */
    listRequest: number;
    /** The modules whose load the user has been told about, and how it is going. */
    moduleLoadStatuses: Record<string, RemoteModuleLoadStatus>;
    setModules: (modules: GammaModule[]) => void;
    setListStatus: (listStatus: ModuleListStatus) => void;
    requestList: () => void;
    setModuleLoadStatus: (moduleId: string, status: RemoteModuleLoadStatus | undefined) => void;
}

export const useModulesStore = create<ModulesState>(set => ({
    modules: [],
    moduleLoadStatuses: {},
    listStatus: 'pending',
    listRequest: 0,
    setModules: modules => set({ modules }),
    setListStatus: listStatus => set({ listStatus }),
    requestList: () => set(state => ({ listRequest: state.listRequest + 1 })),
    setModuleLoadStatus: (moduleId, status) =>
        set(state => {
            const moduleLoadStatuses = { ...state.moduleLoadStatuses };
            if (status) moduleLoadStatuses[moduleId] = status;
            else delete moduleLoadStatuses[moduleId];
            return { moduleLoadStatuses };
        }),
}));
