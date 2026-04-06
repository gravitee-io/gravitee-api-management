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

import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { useAuthStore } from '../../auth/auth.store';
import { type GammaModule, type GammaModuleResponse, parseModule } from '../modules.types';

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

export function useGammaModules(): { modules: GammaModule[]; loading: boolean; error: Error | null } {
    const gammaBaseURL = useBootstrapStore(s => s.config?.gammaBaseURL ?? '');
    const organizationId = useBootstrapStore(s => s.config?.organizationId ?? '');
    const user = useAuthStore(s => s.user);
    const [modules, setModules] = React.useState<GammaModule[]>([]);
    const [loading, setLoading] = React.useState(true);
    const [error, setError] = React.useState<Error | null>(null);

    React.useEffect(() => {
        if (!gammaBaseURL || !organizationId) {
            return;
        }

        if (!user) {
            setModules([]);
            setError(null);
            setLoading(false);
            return;
        }

        const controller = new AbortController();
        setLoading(true);
        setError(null);

        const modulesURL = `${gammaBaseURL}/organizations/${organizationId}/modules`;
        fetch(modulesURL, { credentials: 'include', signal: controller.signal })
            .then(res => {
                if (!res.ok) throw new Error(`Failed to load modules: ${res.status}`);
                return res.json() as Promise<GammaModuleResponse[]>;
            })
            .then(data => {
                const parsed = Array.isArray(data) ? data.map(parseModule) : [];
                const remotes = parsed.map(m => ({
                    name: m.remoteName,
                    entry:
                        DEV_MODULE_ENTRIES[m.id] ??
                        `${gammaBaseURL}/organizations/${organizationId}/modules/${m.id}/assets/mf-manifest.json`,
                }));
                registerRemotes(remotes, { force: true });
                setModules(parsed);
            })
            .catch(err => {
                if (!controller.signal.aborted) {
                    setError(err instanceof Error ? err : new Error(String(err)));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [gammaBaseURL, organizationId, user]);

    return { modules, loading, error };
}
