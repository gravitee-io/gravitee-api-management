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
import { useEffect, useRef } from 'react';
import { type NavigateFunction, Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useEnvironmentStore } from './environment.store';
import type { Environment } from './environment.types';
import { getPrimaryHrid, resolveEnvironmentFromSegment, shouldRewriteIdToHrid, useEnvHrid } from './environment.utils';

function redirectToFirst(env: Environment, navigate: NavigateFunction, search: string, hash: string) {
    navigate({ pathname: `/environments/${getPrimaryHrid(env)}/home`, search, hash }, { replace: true });
}

function rewriteToHrid(env: Environment, pathname: string, navigate: NavigateFunction, search: string, hash: string) {
    const rest = pathname.split('/').filter(Boolean).slice(2);
    const base = `/environments/${getPrimaryHrid(env)}`;
    navigate({ pathname: rest.length > 0 ? `${base}/${rest.join('/')}` : base, search, hash }, { replace: true });
}

/**
 * Synchronizes URL :envHrid with the environment store, canonicalizes id to primary hrid in the URL,
 * and redirects invalid segments to the first available environment.
 */
export function EnvironmentGuard() {
    const envHrid = useEnvHrid();
    const navigate = useNavigate();
    const location = useLocation();
    const locationRef = useRef(location);
    locationRef.current = location;

    const environments = useEnvironmentStore(s => s.environments);
    const setCurrentEnvironment = useEnvironmentStore(s => s.setCurrentEnvironment);
    const lastSyncedId = useRef<string | null>(null);

    useEffect(() => {
        if (!environments.length) return;

        const { search, hash, pathname } = locationRef.current;
        const first = environments[0]!;
        const resolved = resolveEnvironmentFromSegment(environments, envHrid);

        if (!resolved) {
            lastSyncedId.current = null;
            redirectToFirst(first, navigate, search, hash);
            return;
        }

        if (shouldRewriteIdToHrid(resolved, envHrid)) {
            lastSyncedId.current = null;
            rewriteToHrid(resolved, pathname, navigate, search, hash);
            return;
        }

        if (lastSyncedId.current !== resolved.id) {
            lastSyncedId.current = resolved.id;
            setCurrentEnvironment(resolved);
        }
    }, [envHrid, environments, navigate, setCurrentEnvironment]);

    if (!environments.length) {
        return null;
    }

    return <Outlet />;
}
