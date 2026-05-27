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

export const ROUTE_KEYS = [
    // Overview
    'dashboard',
    // Policy Management
    'mcps',
    'llms',
    'apis',
    'custom-policies',
] as const;
export type RouteKey = (typeof ROUTE_KEYS)[number];

export const ROUTES: Record<RouteKey, { readonly path: string; readonly label: string }> = {
    dashboard: { path: 'dashboard', label: 'Dashboard' },
    mcps: { path: 'mcps', label: 'MCPs' },
    llms: { path: 'llms', label: 'AI Models' },
    apis: { path: 'apis', label: 'APIs' },
    'custom-policies': { path: 'custom-policies', label: 'Custom Policies' },
};

export const AUTHZ_ROUTE_CONFIG: ModuleRouteConfig<RouteKey> = {
    routeKeys: ROUTE_KEYS,
    routes: ROUTES,
    defaultRouteKey: 'dashboard',
} as const;
