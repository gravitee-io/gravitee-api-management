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

export const ROUTE_KEYS: readonly string[] = ['dashboard', 'apis', 'api-products', 'applications', 'analytics', 'settings'];
export type RouteKey = (typeof ROUTE_KEYS)[number];

const DEFAULT_ROUTE_KEY: RouteKey = 'apis';

export const ROUTES: Record<RouteKey, { readonly path: string; readonly label: string }> = {
    dashboard: { path: 'dashboard', label: 'Dashboard' },
    apis: { path: 'apis', label: 'API Proxies' },
    'api-products': { path: 'api-products', label: 'API Products' },
    applications: { path: 'applications', label: 'Applications' },
    analytics: { path: 'analytics', label: 'Analytics' },
    settings: { path: 'settings', label: 'Settings' },
};

export const APIM_ROUTE_CONFIG: ModuleRouteConfig<RouteKey> = {
    routeKeys: ROUTE_KEYS,
    routes: ROUTES,
    defaultRouteKey: DEFAULT_ROUTE_KEY,
} as const;
