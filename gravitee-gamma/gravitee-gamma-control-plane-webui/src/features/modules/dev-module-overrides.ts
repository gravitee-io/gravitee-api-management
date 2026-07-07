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

function applyDevSpecToModule(module: GammaModule): GammaModule {
    const spec = DEV_MODULE_SPECS[module.id];
    if (!spec) {
        return module;
    }

    return {
        ...module,
        name: spec.name,
        version: spec.version,
        remoteName: spec.remoteName,
        exposedModule: spec.exposedModule,
    };
}

function shouldUseDevSpec(
    moduleId: string,
    injectUnlistedDevModules: boolean,
    devEntries: Record<string, string>,
): boolean {
    if (!DEV_MODULE_SPECS[moduleId]) {
        return false;
    }

    return injectUnlistedDevModules || Boolean(devEntries[moduleId]);
}

function resolveDevManifestUrl(moduleId: string, devEntries: Record<string, string>): string {
    return devEntries[moduleId] ?? DEV_MODULE_SPECS[moduleId]!.defaultManifestUrl;
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

    const modules: GammaModule[] = [];
    const remotes: Array<{ name: string; entry: string }> = [];

    for (const apiModule of apiModules) {
        if (shouldUseDevSpec(apiModule.id, injectUnlistedDevModules, devEntries)) {
            const devModule = applyDevSpecToModule(apiModule);
            modules.push(devModule);
            remotes.push({
                name: devModule.remoteName,
                entry: resolveDevManifestUrl(apiModule.id, devEntries),
            });
            continue;
        }

        modules.push(apiModule);
        remotes.push({
            name: apiModule.remoteName,
            entry: devEntries[apiModule.id] ?? backendManifestUrl(gammaBaseURL, organizationId, apiModule.id),
        });
    }

    const injectDevSpec = (id: string, manifestUrl: string) => {
        const spec = DEV_MODULE_SPECS[id];
        if (!spec || modules.some(m => m.id === id)) {
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
            injectDevSpec(id, resolveDevManifestUrl(id, devEntries));
        }
    } else {
        for (const [id, entry] of Object.entries(devEntries)) {
            injectDevSpec(id, entry);
        }
    }

    return { modules, remotes };
}
