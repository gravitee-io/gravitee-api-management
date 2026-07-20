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
import { GioDeveloperPortalIcon, UsersIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo } from 'react';
import { useLocation, useNavigate, type NavigateOptions } from 'react-router-dom';

import { usePortalApp } from '../../../app/PortalAppContext';

export type PortalsNavKey = 'portals' | 'tenants';

export const PORTALS_MODULE_ID = 'portals';

export const PORTALS_ROUTE_CONFIG: ModuleRouteConfig<PortalsNavKey> = {
    routeKeys: ['portals', 'tenants'],
    routes: {
        portals: { path: '', label: 'Portals' },
        tenants: { path: 'tenants', label: 'Tenants' },
    },
    defaultRouteKey: 'portals',
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
        globalTenantsPath: useMemo(() => resolvePortalsRoutePath('tenants', context), [context]),
        portalTenantsPath: useCallback(
            (portalId: string) => resolvePortalsRoutePath(`portals/${portalId}/tenants`, context),
            [context],
        ),
        portalTenantDetailPath: useCallback(
            (portalId: string, tenantId: string) =>
                resolvePortalsRoutePath(`portals/${portalId}/tenants/${tenantId}`, context),
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
        label: 'Developer Portals',
        items: [
            { key: 'portals', title: 'Portals', icon: GioDeveloperPortalIcon },
            { key: 'tenants', title: 'Tenants', icon: UsersIcon },
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
