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

export type RemoteModuleLoadStatus = 'delayed' | 'attempting' | 'ready';

type RemoteModuleExport = { default: ComponentType };

/** Module Federation's codes for an entry file or a manifest it could not get. */
const FETCH_FAILURE_CODES = ['#RUNTIME-001', '#RUNTIME-003', '#RUNTIME-008'];

/**
 * Recoverable: a file of the module could not be fetched, as during a rolling restart, and a later attempt can get
 * it. Any other error comes from the module's own code, which every attempt on the same build throws again.
 */
export function isRecoverableLoadError(error: unknown): boolean {
    if (!(error instanceof Error)) return false;
    if (error.name === 'ChunkLoadError') return true;
    // The bundler names a script it could not fetch, but only gives a code to a stylesheet.
    if ('code' in error && error.code === 'CSS_CHUNK_LOAD_FAILED') return true;
    // An entry file that loaded but threw while running also comes as RUNTIME-008. Module Federation does not retry it either.
    if (error.message.includes('ScriptExecutionError')) return false;
    return FETCH_FAILURE_CODES.some(code => error.message.includes(code));
}

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

/**
 * The bundler keeps every file a module has loaded in a list on the page, and each new copy of the module's
 * entry starts by taking what the list holds instead of fetching it. After a failed attempt during a rolling
 * restart, the list can hold files of the build that is going away: the next attempt would run them in place
 * of its own files with the same ids. The list is emptied in place, not deleted: Module Federation keeps the
 * shared libraries an earlier copy has already loaded, and that copy receives its later files through it.
 */
function forgetLoadedFiles(remoteName: string): void {
    // The list is named after the module. The prefix depends on the version of rspack that built it.
    ['webpackChunk', 'rspackChunk'].forEach(prefix => {
        const files = (globalThis as Record<string, unknown>)[`${prefix}${remoteName}`];
        if (Array.isArray(files)) files.length = 0;
    });
}

async function loadOnce(id: string): Promise<RemoteModuleExport> {
    const loaded = await loadRemote<RemoteModuleExport>(id);
    if (!loaded) throw new Error(`Failed to load remote module: ${id}`);
    return loaded;
}

const pendingRetries = new Map<string, () => void>();

function waitBeforeRetry(moduleId: string, ms: number): Promise<void> {
    return new Promise(resolve => {
        const timer = setTimeout(retry, ms);
        function retry() {
            clearTimeout(timer);
            pendingRetries.delete(moduleId);
            resolve();
        }
        pendingRetries.set(moduleId, retry);
    });
}

/** Starts the next attempt of a module now instead of waiting for it. Does nothing while no attempt is waiting. */
export function retryModuleNow(moduleId: string): void {
    pendingRetries.get(moduleId)?.();
}

export async function loadRemoteModule(
    module: GammaModule,
    onStatus?: (status: RemoteModuleLoadStatus) => void,
): Promise<RemoteModuleExport> {
    const id = `${module.remoteName}/${module.exposedModule}`;
    const attempts = RETRY_DELAYS_MS.length + 1;
    const startedAt = Date.now();
    let delayed = false;
    let unrecoverableBefore = false;

    for (let attempt = 1; ; attempt++) {
        if (delayed) onStatus?.('attempting');
        try {
            const loaded = await loadOnce(id);
            if (attempt > 1) console.info(`[Modules] Loaded module "${module.id}" after ${attempt} attempts.`);
            if (delayed) onStatus?.('ready');
            return loaded;
        } catch (error) {
            const unrecoverable = !isRecoverableLoadError(error);
            // One more attempt, in case this one ran files of two builds. Twice in a row, the build itself fails.
            if (unrecoverable && unrecoverableBefore) {
                console.error(`[Modules] Module "${module.id}" failed while starting, twice in a row; giving up.`, error);
                throw error;
            }
            unrecoverableBefore = unrecoverable;
            if (attempt === attempts) {
                console.error(`[Modules] Could not load module "${module.id}" after ${attempts} attempts.`, error);
                throw error;
            }
            let delay = RETRY_DELAYS_MS[attempt - 1];
            if (!delayed && (delay >= DELAY_REPORTED_AFTER_MS || Date.now() - startedAt >= DELAY_REPORTED_AFTER_MS)) {
                delayed = true;
                delay = Math.max(delay, DELAY_REPORTED_AFTER_MS);
            }
            if (delayed) onStatus?.('delayed');
            console.warn(
                unrecoverable
                    ? `[Modules] Module "${module.id}" failed while starting (attempt ${attempt} of ${attempts}); trying once more in ${delay / 1000} s.`
                    : `[Modules] Could not load module "${module.id}" (attempt ${attempt} of ${attempts}). The platform may be updating; retrying in ${delay / 1000} s.`,
                error,
            );
            await waitBeforeRetry(module.id, delay);
            forgetLoadedFiles(module.remoteName);
            registerWithFreshManifest(module.remoteName);
        }
    }
}
