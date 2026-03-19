/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
    mfManifest: { name: string; exposes?: Array<{ name: string; [key: string]: unknown }>; [key: string]: unknown };
}

export interface GammaModule {
    id: string;
    name: string;
    version: string;
    remoteName: string;
    exposedModule: string;
}

export function parseModule(raw: GammaModuleResponse): GammaModule {
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
