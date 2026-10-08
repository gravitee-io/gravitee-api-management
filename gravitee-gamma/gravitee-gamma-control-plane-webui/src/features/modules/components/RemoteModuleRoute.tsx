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
import { lazy, Suspense, type LazyExoticComponent, type ComponentType } from 'react';

import { ModuleUnavailable } from './ModuleUnavailable';
import { ContentSkeleton } from '../../../shared/components/ContentSkeleton';
import { ErrorBoundary } from '../../../shared/components/ErrorBoundary';
import { useEnvironmentStore } from '../../environment/environment.store';
import { getModuleLabel } from '../modules.labels';
import { loadRemoteModule } from '../modules.remotes';
import type { GammaModule } from '../modules.types';

const lazyComponentCache = new Map<string, LazyExoticComponent<ComponentType>>();

export function getOrCreateLazyModule(module: GammaModule): LazyExoticComponent<ComponentType> {
    const cacheKey = `${module.remoteName}/${module.exposedModule}`;
    let cached = lazyComponentCache.get(cacheKey);
    if (!cached) {
        cached = lazy(() =>
            loadRemoteModule(module).catch((error: unknown) => {
                // React keeps a failed lazy component failed: dropping it lets the next visit load the module again.
                lazyComponentCache.delete(cacheKey);
                throw error;
            }),
        );
        lazyComponentCache.set(cacheKey, cached);
    }
    return cached;
}

/**
 * Mounts a federated module, keyed by environment so that switching environments remounts it.
 *
 * Everything a module holds below the SDK -- component state, stores, caches -- is scoped to the
 * environment it was loaded for. Remounting drops it without asking module authors for teardown
 * logic, which matters because modules ship from their own repositories. The technical id is the
 * key rather than the URL segment, so canonicalizing an id to its hrid does not remount.
 * The lazy component itself is cached, so this re-renders the remote without re-fetching it.
 *
 * EnvironmentGuard only renders this once the store environment matches the URL, so the key is
 * always the environment the URL addresses.
 *
 * A module that cannot load or render shows a message in its own place, so the rest of the console
 * stays usable. React Router renders every module route in the same slot, hence the boundary keyed by
 * module: without it, the error of one module would stay on screen when the next one opens.
 *
 * The Suspense must stay inside the boundary: once the load settles, React retries from the closest
 * Suspense with the same lazy component, so a failure reaches the boundary. Retrying from a Suspense
 * above this route would render it again and create a new lazy component, the failed one being
 * dropped from the cache, and the load would start over instead of showing the message.
 */
export function RemoteModuleRoute({ module }: { readonly module: GammaModule }) {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const LazyModule = getOrCreateLazyModule(module);
    const moduleName = getModuleLabel(module.id, module.name);

    return (
        <ErrorBoundary key={module.id} fallback={(_error, reload) => <ModuleUnavailable moduleName={moduleName} onReload={reload} />}>
            <Suspense fallback={<ContentSkeleton />}>
                <LazyModule key={environmentId} />
            </Suspense>
        </ErrorBoundary>
    );
}
