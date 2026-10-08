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
import { lazy, Suspense, useEffect, useRef, type ComponentType, type LazyExoticComponent, type RefObject } from 'react';

import { ModuleUnavailable } from './ModuleUnavailable';
import { ModuleUpdating } from './ModuleUpdating';
import { ContentSkeleton } from '../../../shared/components/ContentSkeleton';
import { ErrorBoundary } from '../../../shared/components/ErrorBoundary';
import { useEnvironmentStore } from '../../environment/environment.store';
import { getModuleLabel } from '../modules.labels';
import { loadRemoteModule, retryModuleNow } from '../modules.remotes';
import { useModulesStore } from '../modules.store';
import type { GammaModule } from '../modules.types';

const lazyComponentCache = new Map<string, LazyExoticComponent<ComponentType>>();

export function getOrCreateLazyModule(module: GammaModule): LazyExoticComponent<ComponentType> {
    const cacheKey = `${module.remoteName}/${module.exposedModule}`;
    let cached = lazyComponentCache.get(cacheKey);
    if (!cached) {
        cached = lazy(() => {
            const { setModuleLoadStatus } = useModulesStore.getState();
            return loadRemoteModule(module, status => setModuleLoadStatus(module.id, status)).catch((error: unknown) => {
                // React keeps a failed lazy component failed: dropping it lets the next visit load the module again.
                lazyComponentCache.delete(cacheKey);
                setModuleLoadStatus(module.id, undefined);
                throw error;
            });
        });
        lazyComponentCache.set(cacheKey, cached);
    }
    return cached;
}

interface ModuleLoadProps {
    readonly moduleId: string;
    readonly moduleName: string;
    readonly focusTarget: RefObject<HTMLDivElement | null>;
}

function ModuleLoadAnnouncement({ moduleId, moduleName, focusTarget }: ModuleLoadProps) {
    const status = useModulesStore(s => s.moduleLoadStatuses[moduleId]);
    let announcement = '';
    if (status === 'ready') announcement = `${moduleName} is ready.`;
    else if (status) announcement = `${moduleName} isn't ready yet. We keep retrying and will open it here as soon as it's ready.`;

    return (
        <div ref={focusTarget} role="status" tabIndex={-1} className="sr-only">
            {announcement}
        </div>
    );
}

function RemoteModuleLoading({ moduleId, moduleName, focusTarget }: ModuleLoadProps) {
    const status = useModulesStore(s => s.moduleLoadStatuses[moduleId]);
    const messageShown = useRef(false);

    useEffect(() => {
        if (status) messageShown.current = true;
    }, [status]);

    useEffect(
        () => () => {
            // The waiting message takes its button, and the focus, away with it when the module opens.
            if (messageShown.current && document.activeElement === document.body) focusTarget.current?.focus();
        },
        [focusTarget],
    );

    if (!status) return <ContentSkeleton />;
    return <ModuleUpdating moduleName={moduleName} attempting={status !== 'delayed'} onRetryNow={() => retryModuleNow(moduleId)} />;
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
 *
 * Loading news is announced in a status region that stays mounted: screen readers often skip a
 * region that appears already filled. The region also takes the focus back when the waiting
 * message goes away with its button.
 */
export function RemoteModuleRoute({ module }: { readonly module: GammaModule }) {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const LazyModule = getOrCreateLazyModule(module);
    const moduleName = getModuleLabel(module.id, module.name);
    const announcement = useRef<HTMLDivElement>(null);

    return (
        <>
            <ModuleLoadAnnouncement moduleId={module.id} moduleName={moduleName} focusTarget={announcement} />
            <ErrorBoundary key={module.id} fallback={(_error, reload) => <ModuleUnavailable moduleName={moduleName} onReload={reload} />}>
                <Suspense fallback={<RemoteModuleLoading moduleId={module.id} moduleName={moduleName} focusTarget={announcement} />}>
                    <LazyModule key={environmentId} />
                </Suspense>
            </ErrorBoundary>
        </>
    );
}
