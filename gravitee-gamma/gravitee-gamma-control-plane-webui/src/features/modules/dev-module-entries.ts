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
import { type GammaModuleResponse, hasUi } from './modules.types';

/** Well-known local Module Federation URLs for core Gamma modules. */
export const LOCAL_DEV_MODULE_FALLBACKS: Readonly<Record<string, string>> = {
    apim: 'http://localhost:3001/mf-manifest.json',
    platform: 'http://localhost:3002/mf-manifest.json',
};

/** Parses `DEV_MODULE_ENTRIES` (`id=url,id=url`) into a plugin-id → manifest-url map. */
export function parseDevModuleEntries(raw: string): Record<string, string> {
    return raw
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
}

/**
 * When any local override is set, also fill in missing core-module URLs. A typical
 * `DEV_MODULE_ENTRIES=apim=...` session then still loads Platform from port 3002.
 * Fetch failures in `attachDevManifests` are ignored.
 */
export function withLocalDevModuleFallbacks(entries: Record<string, string>): Record<string, string> {
    if (Object.keys(entries).length === 0) {
        return entries;
    }
    return { ...LOCAL_DEV_MODULE_FALLBACKS, ...entries };
}

/**
 * When a listed module has no packaged UI, fill `mfManifest` from a local Module Federation
 * override (`DEV_MODULE_ENTRIES`). The Management API can ship backend-only plugins (no `ui/`
 * assets); local module servers still need a route and a remote name.
 */
export async function attachDevManifests(
    modules: readonly GammaModuleResponse[],
    devEntries: Record<string, string>,
    fetchImpl: typeof fetch = fetch,
    signal?: AbortSignal,
): Promise<GammaModuleResponse[]> {
    return Promise.all(
        modules.map(async mod => {
            const entry = devEntries[mod.id];
            if (!entry || hasUi(mod)) {
                return mod;
            }
            try {
                const res = await fetchImpl(entry, { credentials: 'omit', signal });
                if (!res.ok) {
                    return mod;
                }
                const mfManifest: unknown = await res.json();
                if (!isManifest(mfManifest)) {
                    return mod;
                }
                return { ...mod, mfManifest };
            } catch {
                return mod;
            }
        }),
    );
}

function isManifest(value: unknown): value is NonNullable<GammaModuleResponse['mfManifest']> {
    return typeof value === 'object' && value !== null && 'name' in value && typeof (value as { name: unknown }).name === 'string';
}
