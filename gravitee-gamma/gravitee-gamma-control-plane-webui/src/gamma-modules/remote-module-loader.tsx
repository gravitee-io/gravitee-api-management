import React from 'react';
import { loadRemote } from '@module-federation/runtime';
import { GammaModule } from './gamma-module';

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
