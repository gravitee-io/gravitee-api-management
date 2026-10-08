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
import { lazy, type LazyExoticComponent, type ComponentType } from 'react';

import { useEnvironmentStore } from '../../environment/environment.store';
import { loadRemoteModule } from '../modules.remotes';
import type { GammaModule } from '../modules.types';

const lazyComponentCache = new Map<string, LazyExoticComponent<ComponentType>>();

export function getOrCreateLazyModule(module: GammaModule): LazyExoticComponent<ComponentType> {
    const cacheKey = `${module.remoteName}/${module.exposedModule}`;
    let cached = lazyComponentCache.get(cacheKey);
    if (!cached) {
        cached = lazy(() => loadRemoteModule(module));
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
 */
export function RemoteModuleRoute({ module }: { readonly module: GammaModule }) {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const LazyModule = getOrCreateLazyModule(module);

    return <LazyModule key={environmentId} />;
}
