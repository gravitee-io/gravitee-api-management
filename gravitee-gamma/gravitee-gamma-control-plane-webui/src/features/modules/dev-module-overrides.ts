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
import type { GammaModule } from './modules.types';

export interface DevModuleSpec {
    readonly name: string;
    readonly version: string;
    readonly remoteName: string;
    readonly exposedModule: string;
    readonly defaultManifestUrl: string;
}

/**
 * Modules that can be wired from a local MF dev server without a backend gamma-module plugin.
 * Used for POC / local development when the Management API does not list the module yet.
 */
export const DEV_MODULE_SPECS: Record<string, DevModuleSpec> = {
    portals: {
        name: 'Developer Portals',
        version: 'dev',
        remoteName: 'portal_gamma',
        exposedModule: 'App',
        defaultManifestUrl: 'http://localhost:4103/portal-editor/mf-manifest.json',
    },
};

export function parseDevModuleEntries(raw: string | undefined): Record<string, string> {
    return (raw ?? '')
        .split(',')
        .filter(Boolean)
        .reduce(
            (acc, entry) => {
                const [id, url] = entry.split('=', 2);
                if (id && url) {
                    acc[id.trim()] = url.trim();
                }
                return acc;
            },
            {} as Record<string, string>,
        );
}

function backendManifestUrl(gammaBaseURL: string, organizationId: string, moduleId: string): string {
    return `${gammaBaseURL}/organizations/${organizationId}/modules/${moduleId}/assets/mf-manifest.json`;
}

export function resolveGammaModules(
    apiModules: readonly GammaModule[],
    options: {
        readonly devEntries: Record<string, string>;
        readonly gammaBaseURL: string;
        readonly organizationId: string;
        /** When true, inject {@link DEV_MODULE_SPECS} not returned by the API (dev / POC). */
        readonly injectUnlistedDevModules: boolean;
    },
): { modules: GammaModule[]; remotes: Array<{ name: string; entry: string }> } {
    const { devEntries, gammaBaseURL, organizationId, injectUnlistedDevModules } = options;
    const apiIds = new Set(apiModules.map(m => m.id));

    const modules = [...apiModules];
    const remotes = apiModules.map(m => ({
        name: m.remoteName,
        entry: devEntries[m.id] ?? backendManifestUrl(gammaBaseURL, organizationId, m.id),
    }));

    const injectDevSpec = (id: string, manifestUrl: string) => {
        const spec = DEV_MODULE_SPECS[id];
        if (!spec || apiIds.has(id) || modules.some(m => m.id === id)) {
            return;
        }
        modules.push({
            id,
            name: spec.name,
            version: spec.version,
            remoteName: spec.remoteName,
            exposedModule: spec.exposedModule,
        });
        remotes.push({ name: spec.remoteName, entry: manifestUrl });
    };

    if (injectUnlistedDevModules) {
        for (const id of Object.keys(DEV_MODULE_SPECS)) {
            injectDevSpec(id, devEntries[id] ?? DEV_MODULE_SPECS[id].defaultManifestUrl);
        }
    } else {
        for (const [id, entry] of Object.entries(devEntries)) {
            injectDevSpec(id, entry);
        }
    }

    return { modules, remotes };
}
