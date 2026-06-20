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
import type { ModuleRouteConfig } from '@gravitee/gamma-modules-sdk/routing';

import { observability } from './observability';

export const ROUTE_KEYS = [
    'dashboard',
    'apis',
    'api-products',
    'analytics',
    'settings',
    'observe/dashboards',
    'observe/logs',
    'observe/tracing',
] as const;
export type RouteKey = (typeof ROUTE_KEYS)[number];

const ROUTE_KEY_SET = new Set<string>(ROUTE_KEYS);
const DEFAULT_ROUTE_KEY: RouteKey = 'dashboard';

export const ROUTES: Record<RouteKey, { readonly path: string; readonly label: string }> = {
    dashboard: { path: '', label: 'Dashboard' },
    apis: { path: 'apis', label: 'API Proxies' },
    'api-products': { path: 'api-products', label: 'API Products' },
    analytics: { path: 'analytics', label: 'Analytics' },
    settings: { path: 'settings', label: 'Settings' },
    'observe/dashboards': { path: 'observe/dashboards', label: 'Dashboards' },
    'observe/logs': { path: 'observe/logs', label: 'Logs' },
    'observe/tracing': { path: 'observe/tracing', label: 'Tracing' },
};

export function isRouteKey(segment: string): segment is RouteKey {
    return ROUTE_KEY_SET.has(segment);
}

/** Config consumed by `@gravitee/gamma-modules-sdk/routing` helpers. */
export const APIM_ROUTE_CONFIG: ModuleRouteConfig<RouteKey> = {
    routeKeys: ROUTE_KEYS,
    routes: ROUTES,
    defaultRouteKey: DEFAULT_ROUTE_KEY,
} as const;

/** Index of the first segment that belongs to the module (i.e. after its mount prefix). */
function moduleRelativeStartIndex(segments: readonly string[], modulePrefix?: string): number {
    const prefix = (modulePrefix ?? '').split('/').filter(Boolean);
    if (prefix.length === 0) return 0;
    for (let i = 0; i + prefix.length <= segments.length; i++) {
        if (prefix.every((seg, j) => segments[i + j] === seg)) {
            return i + prefix.length;
        }
    }
    return 0;
}

/**
 * Resolves the active sidebar key from a URL pathname.
 *
 * Observability composite keys (`observe/*`) are reparsed by the lib itself via
 * `observability.resolveRouteKey`. Other keys are matched by scanning segments.
 *
 * Scanning is scoped to the module-relative path (everything after `modulePrefix`, e.g. the host
 * `environments/{hrid}/{module}` mount) and returns the first/outermost route-key segment, so:
 *  - the top-level section wins over nested sub-routes sharing a route-key name — an API Product's
 *    APIs tab (`api-products/:productId/apis`) resolves to `api-products`, not `apis`; and
 *  - a host segment such as an environment hrid that happens to match a route-key name cannot win.
 */
export function getActiveNavKey(pathname: string, modulePrefix?: string): RouteKey {
    const observabilityKey = observability.resolveRouteKey(pathname);
    if (observabilityKey !== null && isRouteKey(observabilityKey)) return observabilityKey;

    const segments = pathname.split('/').filter(Boolean);
    for (let i = moduleRelativeStartIndex(segments, modulePrefix); i < segments.length; i++) {
        if (isRouteKey(segments[i])) {
            return segments[i] as RouteKey;
        }
    }
    return DEFAULT_ROUTE_KEY;
}
