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
import { environmentService } from '@gravitee/gamma-modules-sdk';

import { useEnvironmentStore } from './environment.store';

/**
 * Subscribes to the Zustand environment store and pushes changes to the
 * SDK environmentService singleton so federated modules can access the
 * current environment via useEnvironment().
 */
export function startEnvironmentSync(): () => void {
    return useEnvironmentStore.subscribe((state, prev) => {
        if (state.currentEnvironment !== prev.currentEnvironment) {
            environmentService.setEnvironment(state.currentEnvironment);
        }
    });
}
