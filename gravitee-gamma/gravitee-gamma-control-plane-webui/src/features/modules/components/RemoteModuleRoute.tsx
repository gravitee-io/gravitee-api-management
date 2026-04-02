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
import { loadRemote } from '@module-federation/runtime';
import React from 'react';

import type { GammaModule } from '../modules.types';

const lazyComponentCache = new Map<string, React.LazyExoticComponent<React.ComponentType>>();

export function getOrCreateLazyModule(remoteName: string, exposedModule: string): React.LazyExoticComponent<React.ComponentType> {
    const cacheKey = `${remoteName}/${exposedModule}`;
    let cached = lazyComponentCache.get(cacheKey);
    if (!cached) {
        cached = React.lazy(async () => {
            const mod = await loadRemote<{ default: React.ComponentType }>(`${remoteName}/${exposedModule}`);
            if (!mod) throw new Error(`Failed to load remote module: ${remoteName}/${exposedModule}`);
            return mod;
        });
        lazyComponentCache.set(cacheKey, cached);
    }
    return cached;
}

export function RemoteModuleRoute({ module }: { readonly module: GammaModule }) {
    const LazyModule = getOrCreateLazyModule(module.remoteName, module.exposedModule);
    return <LazyModule />;
}
