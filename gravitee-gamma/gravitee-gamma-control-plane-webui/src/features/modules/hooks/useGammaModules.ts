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
import { useState, useEffect } from 'react';

import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { useAuthStore } from '../../auth/auth.store';
import { attachDevManifests, parseDevModuleEntries, withLocalDevModuleFallbacks } from '../dev-module-entries';
import { useModulesStore } from '../modules.store';
import { type GammaModule, type GammaModuleResponse, hasUi, parseModule } from '../modules.types';

export function useGammaModules(): { modules: GammaModule[]; loading: boolean; error: Error | null; retry: () => void } {
    const gammaBaseURL = useBootstrapStore(s => s.config?.gammaBaseURL ?? '');
    const organizationId = useBootstrapStore(s => s.config?.organizationId ?? '');
    const user = useAuthStore(s => s.user);
    const modules = useModulesStore(s => s.modules);
    const setModules = useModulesStore(s => s.setModules);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<Error | null>(null);
    const [retryCount, setRetryCount] = useState(0);

    const retry = () => setRetryCount(c => c + 1);

    useEffect(() => {
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
        const devEntries = withLocalDevModuleFallbacks(parseDevModuleEntries(process.env.DEV_MODULE_ENTRIES ?? ''));
        fetch(modulesURL, { credentials: 'include', signal: controller.signal })
            .then(res => {
                if (!res.ok) throw new Error(`Failed to load modules: ${res.status}`);
                return res.json() as Promise<GammaModuleResponse[]>;
            })
            .then(async data => {
                const listed = Array.isArray(data) ? data : [];
                const hydrated = await attachDevManifests(listed, devEntries, fetch, controller.signal);
                if (controller.signal.aborted) {
                    return;
                }
                const parsed = hydrated.filter(hasUi).map(parseModule);
                const remotes = parsed.map(m => ({
                    name: m.remoteName,
                    entry: devEntries[m.id] ?? `${gammaBaseURL}/organizations/${organizationId}/modules/${m.id}/assets/mf-manifest.json`,
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
    }, [gammaBaseURL, organizationId, user, retryCount]);

    return { modules, loading, error, retry };
}
