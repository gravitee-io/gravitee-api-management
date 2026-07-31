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
/** Sidebar / route keys for the host shell area (home, tasks, …). */
export type HostNavKey = 'home' | 'tasks';

export const HOME_NAV_KEY: HostNavKey = 'home';
export const TASKS_NAV_KEY: HostNavKey = 'tasks';

/** Labels for sidebar titles and breadcrumbs (single source of truth). */
export const HOST_NAV_LABELS: Record<HostNavKey, string> = {
    home: 'Home',
    tasks: 'Tasks & Approvals',
};

/**
 * Path for a host nav key under a given environment segment (hrid or id in URL).
 * @example hostNavPath('home', 'default') -> '/environments/default/home'
 */
export function hostNavPath(navKey: HostNavKey, envHrid: string): string {
    return `/environments/${envHrid}/${navKey}`;
}

export interface HostBreadcrumbSegment {
    readonly label: string;
    readonly to?: string;
}

interface HostNavArea {
    readonly navKey: HostNavKey;
    readonly matches: (subPath: string) => boolean;
    readonly breadcrumbSegments: (envHrid: string) => readonly HostBreadcrumbSegment[];
}

const HOST_NAV_AREAS: readonly HostNavArea[] = [
    {
        navKey: TASKS_NAV_KEY,
        matches: sub => sub === TASKS_NAV_KEY || sub.startsWith(`${TASKS_NAV_KEY}/`),
        breadcrumbSegments: () => [{ label: HOST_NAV_LABELS.tasks }],
    },
    {
        navKey: HOME_NAV_KEY,
        matches: sub => sub === HOME_NAV_KEY || sub === '' || sub.startsWith(`${HOME_NAV_KEY}/`),
        breadcrumbSegments: () => [{ label: HOST_NAV_LABELS.home }],
    },
];

/**
 * Path segments after /environments/:envHrid (e.g. ['apim', 'x'] for .../environments/e/apim/x).
 * Empty when the pathname is not under that environment prefix.
 */
export function pathSegmentsAfterEnvironment(pathname: string, envHrid: string): string[] {
    const prefix = `/environments/${envHrid}`;
    if (!pathname.startsWith(prefix)) {
        return [];
    }
    const tail = pathname.slice(prefix.length);
    return tail.split('/').filter(Boolean);
}

/**
 * New pathname when changing the environment segment while keeping the same page
 * (host area, module path, or nested route). If there is no path under the current
 * environment, returns /environments/{new}/home.
 */
export function buildPathnameAfterEnvironmentChange(pathname: string, currentEnvHrid: string, newEnvHrid: string): string {
    const rest = pathSegmentsAfterEnvironment(pathname, currentEnvHrid);
    const base = `/environments/${newEnvHrid}`;
    return rest.length > 0 ? `${base}/${rest.join('/')}` : `${base}/home`;
}

function extractSubPath(pathname: string, envHrid: string): string | null {
    const prefix = `/environments/${envHrid}`;
    if (!pathname.startsWith(prefix)) return null;
    const tail = pathname.slice(prefix.length);
    if (tail === '' || tail === '/') return '';
    if (tail.startsWith('/')) return tail.slice(1);
    return null;
}

export function resolveHostRoute(
    pathname: string,
    envHrid: string,
): {
    activeNavKey: HostNavKey;
    breadcrumbSegments: readonly HostBreadcrumbSegment[];
} {
    const defaultResult = {
        activeNavKey: HOME_NAV_KEY,
        breadcrumbSegments: [{ label: HOST_NAV_LABELS.home, to: hostNavPath(HOME_NAV_KEY, envHrid) }] as readonly HostBreadcrumbSegment[],
    };

    const subPath = extractSubPath(pathname, envHrid);
    if (subPath === null) return defaultResult;

    for (const area of HOST_NAV_AREAS) {
        if (area.matches(subPath)) {
            return { activeNavKey: area.navKey, breadcrumbSegments: area.breadcrumbSegments(envHrid) };
        }
    }
    return defaultResult;
}

export function isHostNavKey(key: string): key is HostNavKey {
    return key === HOME_NAV_KEY || key === TASKS_NAV_KEY;
}
