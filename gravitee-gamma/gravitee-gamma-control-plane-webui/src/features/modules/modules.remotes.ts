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
import { loadRemote, registerRemotes } from '@module-federation/runtime';
import type { ComponentType } from 'react';

import type { GammaModule } from './modules.types';

/**
 * Waits before each new attempt, about three minutes in total: while the Management API restarts, its pods
 * serve two builds and a module's files can 404 until the new build serves them all. Capping the wait at 15 s
 * opens the module soon after the platform is back.
 */
export const RETRY_DELAYS_MS = [2_000, 5_000, 10_000, ...Array.from({ length: 11 }, () => 15_000)];

/**
 * A load that recovers sooner stays behind the loading skeleton, without a word on screen. Once a load is
 * reported as delayed, the next attempt waits at least this long, so the message shown for it never flashes.
 */
export const DELAY_REPORTED_AFTER_MS = 10_000;

export type RemoteModuleLoadStatus = 'delayed';

type RemoteModuleExport = { default: ComponentType };

const DEV_MODULE_ENTRIES: Record<string, string> = (process.env.DEV_MODULE_ENTRIES ?? '')
    .split(',')
    .filter(Boolean)
    .reduce(
        (acc, entry) => {
            const [id, url] = entry.split('=', 2);
            if (id && url) acc[id] = url;
            return acc;
        },
        {} as Record<string, string>,
    );

const manifestUrls = new Map<string, string>();

export function registerModuleRemotes(modules: readonly GammaModule[], gammaBaseURL: string, organizationId: string): void {
    const remotes = modules.map(m => ({
        name: m.remoteName,
        entry: DEV_MODULE_ENTRIES[m.id] ?? `${gammaBaseURL}/organizations/${organizationId}/modules/${m.id}/assets/mf-manifest.json`,
    }));
    remotes.forEach(remote => manifestUrls.set(remote.name, remote.entry));
    registerRemotes(remotes, { force: true });
}

/**
 * A forced registration under the same URL can keep the manifest read before (Module Federation 0.18 does),
 * and a rolling restart may have made it stale. The query leaves the module's files where they are: their
 * URLs are resolved from the manifest URL without it.
 */
function registerWithFreshManifest(remoteName: string): void {
    const manifestUrl = manifestUrls.get(remoteName);
    if (!manifestUrl) return;
    const freshManifestUrl = new URL(manifestUrl, window.location.href);
    freshManifestUrl.searchParams.set('retry', String(Date.now()));
    registerRemotes([{ name: remoteName, entry: freshManifestUrl.toString() }], { force: true });
}

async function loadOnce(id: string): Promise<RemoteModuleExport> {
    const loaded = await loadRemote<RemoteModuleExport>(id);
    if (!loaded) throw new Error(`Failed to load remote module: ${id}`);
    return loaded;
}

function wait(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
}

export async function loadRemoteModule(
    module: GammaModule,
    onStatus?: (status: RemoteModuleLoadStatus) => void,
): Promise<RemoteModuleExport> {
    const id = `${module.remoteName}/${module.exposedModule}`;
    const attempts = RETRY_DELAYS_MS.length + 1;
    const startedAt = Date.now();
    let delayed = false;

    for (let attempt = 1; ; attempt++) {
        try {
            const loaded = await loadOnce(id);
            if (attempt > 1) console.info(`[Modules] Loaded module "${module.id}" after ${attempt} attempts.`);
            return loaded;
        } catch (error) {
            if (attempt === attempts) {
                console.error(`[Modules] Could not load module "${module.id}" after ${attempts} attempts.`, error);
                throw error;
            }
            let delay = RETRY_DELAYS_MS[attempt - 1];
            if (!delayed && (delay >= DELAY_REPORTED_AFTER_MS || Date.now() - startedAt >= DELAY_REPORTED_AFTER_MS)) {
                delayed = true;
                delay = Math.max(delay, DELAY_REPORTED_AFTER_MS);
                onStatus?.('delayed');
            }
            console.warn(
                `[Modules] Could not load module "${module.id}" (attempt ${attempt} of ${attempts}). The platform may be updating; retrying in ${delay / 1000} s.`,
                error,
            );
            await wait(delay);
            registerWithFreshManifest(module.remoteName);
        }
    }
}
