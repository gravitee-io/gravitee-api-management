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
import {
    buildModuleNavPath,
    resolveModulePath,
    type ModuleRouteConfig,
} from '@gravitee/gamma-modules-sdk/routing';
import type { NavGroup } from '@gravitee/graphene-core';
import {
    BarChart3Icon,
    ChartLineIcon,
    FileTextIcon,
    GlobeIcon,
    HomeIcon,
    LayoutGridIcon,
    PuzzleIcon,
    UsersIcon,
    WebhookIcon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useMemo } from 'react';
import { useLocation, useNavigate, type NavigateOptions } from 'react-router-dom';

import { usePortalApp } from '../../../app/PortalAppContext';

export type PortalsNavKey =
    | 'overview'
    | 'identity-providers'
    | 'domains'
    | 'templates'
    | 'google-analytics'
    | 'webhooks'
    | 'third-party-apps'
    | 'dashboards'
    | 'logs';

/** Sidebar sections that render a stub page (everything except Overview). */
export const PORTALS_STUB_NAV_KEYS = [
    'identity-providers',
    'domains',
    'templates',
    'google-analytics',
    'webhooks',
    'third-party-apps',
    'dashboards',
    'logs',
] as const satisfies readonly PortalsNavKey[];

export type PortalsStubNavKey = (typeof PORTALS_STUB_NAV_KEYS)[number];

export const PORTALS_MODULE_ID = 'portals';

export const PORTALS_ROUTE_CONFIG: ModuleRouteConfig<PortalsNavKey> = {
    routeKeys: [
        'identity-providers',
        'domains',
        'templates',
        'google-analytics',
        'webhooks',
        'third-party-apps',
        'dashboards',
        'logs',
        'overview',
    ],
    routes: {
        overview: { path: '', label: 'Overview' },
        'identity-providers': { path: 'identity-providers', label: 'Identity Providers' },
        domains: { path: 'domains', label: 'Domains' },
        templates: { path: 'templates', label: 'Templates' },
        'google-analytics': { path: 'google-analytics', label: 'Google Analytics' },
        webhooks: { path: 'webhooks', label: 'Webhooks' },
        'third-party-apps': { path: 'third-party-apps', label: 'Third-Party Apps' },
        dashboards: { path: 'dashboards', label: 'Dashboards' },
        logs: { path: 'logs', label: 'Logs' },
    },
    defaultRouteKey: 'overview',
};

export interface PortalsRouteContext {
    readonly embeddedInConsole: boolean;
    readonly pathname: string;
}

function toStandaloneRoutePath(routePath: string): string {
    const normalized = routePath.startsWith('/') ? routePath : `/${routePath}`;
    return normalized || '/';
}

export function resolvePortalsRoutePath(routePath: string, context: PortalsRouteContext): string {
    const stripped = routePath.replace(/^\//, '');

    if (!context.embeddedInConsole) {
        return toStandaloneRoutePath(stripped);
    }

    return buildModuleNavPath(PORTALS_MODULE_ID, stripped, context.pathname);
}

export function resolvePortalsHomePath(context: PortalsRouteContext): string {
    if (!context.embeddedInConsole) {
        return '/';
    }

    return buildModuleNavPath(PORTALS_MODULE_ID, '', context.pathname);
}

export function resolvePortalViewPath(
    portalId: string,
    context: PortalsRouteContext,
    query?: Record<string, string>,
): string {
    const path = resolvePortalsRoutePath(`portals/${portalId}`, context);
    if (!query || Object.keys(query).length === 0) {
        return path;
    }

    const params = new URLSearchParams(query);
    return `${path}?${params.toString()}`;
}

export function usePortalsNavigation() {
    const { embeddedInConsole } = usePortalApp();
    const { pathname } = useLocation();
    const navigate = useNavigate();

    const context = useMemo(
        (): PortalsRouteContext => ({ embeddedInConsole, pathname }),
        [embeddedInConsole, pathname],
    );

    const to = useCallback((routePath: string) => resolvePortalsRoutePath(routePath, context), [context]);

    const navigateTo = useCallback(
        (routePath: string, options?: NavigateOptions) => {
            navigate(to(routePath), options);
        },
        [navigate, to],
    );

    return {
        to,
        navigateTo,
        homePath: useMemo(() => resolvePortalsHomePath(context), [context]),
        portalTenantsPath: useCallback(
            (portalId: string) => resolvePortalsRoutePath(`portals/${portalId}/settings/tenants`, context),
            [context],
        ),
        portalTenantDetailPath: useCallback(
            (portalId: string, tenantId: string) =>
                resolvePortalsRoutePath(`portals/${portalId}/settings/tenants/${tenantId}`, context),
            [context],
        ),
        portalSettingsPath: useCallback(
            (portalId: string) => resolvePortalsRoutePath(`portals/${portalId}/settings`, context),
            [context],
        ),
        portalSettingsSectionPath: useCallback(
            (portalId: string, sectionPath: string) =>
                resolvePortalsRoutePath(`portals/${portalId}/settings/${sectionPath}`, context),
            [context],
        ),
        portalViewPath: useCallback(
            (portalId: string, query?: Record<string, string>) => resolvePortalViewPath(portalId, context, query),
            [context],
        ),
    };
}

export const PORTALS_NAV_GROUPS: NavGroup[] = [
    {
        label: 'General',
        items: [{ key: 'overview', title: 'Overview', icon: HomeIcon }],
    },
    {
        label: 'Configuration',
        items: [
            { key: 'identity-providers', title: 'Identity Providers', icon: UsersIcon },
            { key: 'domains', title: 'Domains', icon: GlobeIcon },
            { key: 'templates', title: 'Templates', icon: LayoutGridIcon },
        ],
    },
    {
        label: 'Integrations',
        items: [
            { key: 'google-analytics', title: 'Google Analytics', icon: ChartLineIcon },
            { key: 'webhooks', title: 'Webhooks', icon: WebhookIcon },
            { key: 'third-party-apps', title: 'Third-Party Apps', icon: PuzzleIcon },
        ],
    },
    {
        label: 'Observability',
        items: [
            { key: 'dashboards', title: 'Dashboards', icon: BarChart3Icon },
            { key: 'logs', title: 'Logs', icon: FileTextIcon },
        ],
    },
];

export function getActivePortalsNavKey(pathname: string): PortalsNavKey {
    return resolveModulePath(pathname, PORTALS_ROUTE_CONFIG).activeNavKey;
}

export function isPortalPreviewRoute(pathname: string): boolean {
    const portalViewPattern = /\/portals\/[^/]+(\/[^/]+)?$/;
    return (
        portalViewPattern.test(pathname)
        && !pathname.includes('/tenants')
        && !pathname.includes('/settings')
        && !pathname.endsWith('/edit')
    );
}
