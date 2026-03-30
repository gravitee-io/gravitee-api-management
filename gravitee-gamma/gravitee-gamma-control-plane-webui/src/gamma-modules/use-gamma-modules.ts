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
import React from 'react';

import { GammaModule, GammaModuleResponse, parseModule } from './gamma-module';
import { useBootstrap } from '../bootstrap/bootstrap-context';

export function useGammaModules(): { modules: GammaModule[]; loading: boolean; error: Error | null } {
    const { gammaBaseURL, organizationId } = useBootstrap();
    const [modules, setModules] = React.useState<GammaModule[]>([]);
    const [loading, setLoading] = React.useState(true);
    const [error, setError] = React.useState<Error | null>(null);

    const lastModulesFetchRef = React.useRef<string | null>(null);
    React.useEffect(() => {
        const key = `${gammaBaseURL}|${organizationId}`;
        if (lastModulesFetchRef.current === key) return;
        lastModulesFetchRef.current = key;

        const modulesURL = `${gammaBaseURL}/organizations/${organizationId}/modules`;
        fetch(modulesURL, { credentials: 'include' })
            .then(res => {
                if (!res.ok) throw new Error(`Failed to load modules: ${res.status}`);
                return res.json() as Promise<GammaModuleResponse[]>;
            })
            .then(data => {
                const parsed = Array.isArray(data) ? data.map(parseModule) : [];
                const remotes = parsed.map(m => ({
                    name: m.remoteName,
                    entry: `${gammaBaseURL}/organizations/${organizationId}/modules/${m.id}/assets/mf-manifest.json`,
                }));
                registerRemotes(remotes, { force: true });
                setModules(parsed);
            })
            .catch(err => setError(err instanceof Error ? err : new Error(String(err))))
            .finally(() => setLoading(false));
    }, [gammaBaseURL, organizationId]);

    return { modules, loading, error };
}
