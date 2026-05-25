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
export interface GammaModuleResponse {
    id: string;
    name: string;
    version: string;
    /** Module Federation manifest. Absent for backend-only modules that ship no UI (e.g. authz). */
    mfManifest?: { name: string; exposes?: Array<{ name: string; [key: string]: unknown }>; [key: string]: unknown };
}

export interface GammaModule {
    id: string;
    name: string;
    version: string;
    remoteName: string;
    exposedModule: string;
}

/** A module that ships a UI, i.e. exposes a Module Federation manifest. */
export type UiGammaModuleResponse = GammaModuleResponse & { mfManifest: NonNullable<GammaModuleResponse['mfManifest']> };

/** Type guard keeping only modules that ship a UI; backend-only modules are filtered out.
 *  Checks both `null` and `undefined` explicitly — the field is typed `?:` (so TS says
 *  `undefined`) but the backend may serialize an absent value as `null`. */
export function hasUi(raw: GammaModuleResponse): raw is UiGammaModuleResponse {
    return raw.mfManifest !== undefined && raw.mfManifest !== null;
}

export function parseModule(raw: UiGammaModuleResponse): GammaModule {
    const firstExpose = raw.mfManifest.exposes?.[0]?.name;
    const exposedModule = firstExpose ? firstExpose.replace(/^\.\//, '') : 'Module';

    return {
        id: raw.id,
        name: raw.name,
        version: raw.version,
        remoteName: raw.mfManifest.name,
        exposedModule,
    };
}
