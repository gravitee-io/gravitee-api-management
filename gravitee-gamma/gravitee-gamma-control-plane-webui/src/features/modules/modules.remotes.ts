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
import { registerRemotes } from '@module-federation/runtime';

import type { GammaModule } from './modules.types';

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

export function registerModuleRemotes(modules: readonly GammaModule[], gammaBaseURL: string, organizationId: string): void {
    const remotes = modules.map(m => ({
        name: m.remoteName,
        entry: DEV_MODULE_ENTRIES[m.id] ?? `${gammaBaseURL}/organizations/${organizationId}/modules/${m.id}/assets/mf-manifest.json`,
    }));
    registerRemotes(remotes, { force: true });
}
