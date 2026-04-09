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

/** Sidebar / route keys for the host shell area (home, about, …). */
export type HostNavKey = 'home' | 'about';

export const DEFAULT_HOST_NAV_KEY: HostNavKey = 'home';

/** Labels for sidebar titles and breadcrumbs (single source of truth). */
export const HOST_NAV_LABELS: Record<HostNavKey, string> = {
    home: 'Home',
    about: 'About',
};

/** React Router paths for each nav key. */
export const HOST_NAV_PATHS: Record<HostNavKey, string> = {
    home: '/',
    about: '/about',
};

export interface HostBreadcrumbSegment {
    readonly label: string;
    readonly to?: string;
}

interface HostNavArea {
    readonly navKey: HostNavKey;
    readonly matches: (pathname: string) => boolean;
    readonly breadcrumbSegments: readonly HostBreadcrumbSegment[];
}

/**
 * Most specific matchers first. Extend this list as host routes grow; keep
 * `matches` disjoint or ordered so the first win is intentional.
 */
const HOST_NAV_AREAS: readonly HostNavArea[] = [
    {
        navKey: 'about',
        matches: p => p === '/about' || p.startsWith('/about/'),
        breadcrumbSegments: [{ label: HOST_NAV_LABELS.home, to: HOST_NAV_PATHS.home }, { label: HOST_NAV_LABELS.about }],
    },
    {
        navKey: 'home',
        matches: p => p === '/' || p === '',
        breadcrumbSegments: [{ label: HOST_NAV_LABELS.home }],
    },
];

export function resolveHostRoute(pathname: string): {
    activeNavKey: HostNavKey;
    breadcrumbSegments: readonly HostBreadcrumbSegment[];
} {
    for (const area of HOST_NAV_AREAS) {
        if (area.matches(pathname)) {
            return { activeNavKey: area.navKey, breadcrumbSegments: area.breadcrumbSegments };
        }
    }
    return {
        activeNavKey: DEFAULT_HOST_NAV_KEY,
        breadcrumbSegments: [{ label: HOST_NAV_LABELS.home }],
    };
}

export function isHostNavKey(key: string): key is HostNavKey {
    return key === 'home' || key === 'about';
}
