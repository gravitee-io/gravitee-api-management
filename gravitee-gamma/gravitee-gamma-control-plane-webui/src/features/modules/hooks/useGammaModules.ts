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
import { parseDevModuleEntries, resolveGammaModules } from '../dev-module-overrides';
import { useModulesStore } from '../modules.store';
import { type GammaModule, type GammaModuleResponse, hasUi, parseModule } from '../modules.types';

const DEV_MODULE_ENTRIES = parseDevModuleEntries(process.env.DEV_MODULE_ENTRIES);
const INJECT_UNLISTED_DEV_MODULES = process.env.NODE_ENV === 'development';

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
        fetch(modulesURL, { credentials: 'include', signal: controller.signal })
            .then(res => {
                if (!res.ok) throw new Error(`Failed to load modules: ${res.status}`);
                return res.json() as Promise<GammaModuleResponse[]>;
            })
            .then(data => {
                const parsed = Array.isArray(data) ? data.filter(hasUi).map(parseModule) : [];
                const { modules, remotes } = resolveGammaModules(parsed, {
                    devEntries: DEV_MODULE_ENTRIES,
                    gammaBaseURL,
                    organizationId,
                    injectUnlistedDevModules: INJECT_UNLISTED_DEV_MODULES,
                });
                registerRemotes(remotes, { force: true });
                setModules(modules);
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
