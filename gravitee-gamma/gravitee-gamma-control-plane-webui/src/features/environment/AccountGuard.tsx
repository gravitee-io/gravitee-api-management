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
import { Outlet } from 'react-router-dom';

import { useEnvironmentStore } from './environment.store';
import { EnvironmentsLoadFailed } from './EnvironmentsLoadFailed';
import { ContentSkeleton } from '../../shared/components/ContentSkeleton';

/**
 * For pages above the environments: no environment in the URL, but the session still runs in one,
 * so they wait for the store to hold it.
 */
export function AccountGuard() {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const loadFailed = useEnvironmentStore(s => s.loadFailed);
    const initialized = useEnvironmentStore(s => s.initialized);

    if (!environmentId) {
        if (loadFailed) {
            return <EnvironmentsLoadFailed />;
        }
        // An empty list is an answer: nothing is left to wait for, as with EnvironmentGuard.
        return initialized ? null : <ContentSkeleton />;
    }

    return <Outlet />;
}
