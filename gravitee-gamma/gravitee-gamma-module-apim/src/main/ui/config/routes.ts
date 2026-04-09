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
import type { NavigateFunction } from 'react-router-dom';

export const ROUTE_KEYS = ['apis', 'applications', 'analytics', 'settings'] as const;
export type RouteKey = (typeof ROUTE_KEYS)[number];

const ROUTE_KEY_SET = new Set<string>(ROUTE_KEYS);
const DEFAULT_ROUTE_KEY = 'apis';

export const ROUTES: Record<RouteKey, { readonly path: string; readonly label: string }> = {
    apis: { path: 'apis', label: 'APIs' },
    applications: { path: 'applications', label: 'Applications' },
    analytics: { path: 'analytics', label: 'Analytics' },
    settings: { path: 'settings', label: 'Settings' },
};

export function isRouteKey(segment: string): segment is RouteKey {
    return ROUTE_KEY_SET.has(segment);
}

/**
 * Resolves the active sidebar key and optional host module prefix from the URL.
 */
export function resolveModulePath(pathname: string): { modulePrefix: string; activeNavKey: RouteKey } {
    const segments = pathname.split('/').filter(Boolean);
    if (segments.length === 0) {
        return { modulePrefix: '', activeNavKey: DEFAULT_ROUTE_KEY };
    }
    if (isRouteKey(segments[0])) {
        return { modulePrefix: '', activeNavKey: segments[0] };
    }
    const moduleId = segments[0];
    const sub = segments[1] ?? DEFAULT_ROUTE_KEY;
    const activeNavKey = isRouteKey(sub) ? sub : DEFAULT_ROUTE_KEY;
    return { modulePrefix: moduleId, activeNavKey };
}

/**
 * Navigates to the URL for a sidebar item key.
 * With a host `modulePrefix` (federated), paths are `/{modulePrefix}/{key}`.
 * In standalone mode, the default key `apis` maps to `/`.
 */
export function navigateToNavKey(navigate: NavigateFunction, modulePrefix: string, key: string): void {
    if (modulePrefix) {
        navigate(`/${modulePrefix}/${key}`);
        return;
    }
    navigate(key === 'apis' ? '/' : `/${key}`);
}
