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
    CLOUD_SETTINGS_NAV_LABELS,
    cloudSettingsNavPath,
    resolveCloudSettingsNavKey,
} from './cloud-settings-navigation';

/** Sidebar / route keys for the Cloud area (dashboard, settings, …). */
export type CloudNavKey = 'dashboard' | 'settings';

export const CLOUD_DASHBOARD_NAV_KEY: CloudNavKey = 'dashboard';
export const CLOUD_SETTINGS_NAV_KEY: CloudNavKey = 'settings';

export const CLOUD_NAV_LABELS: Record<CloudNavKey, string> = {
    dashboard: 'Dashboard',
    settings: 'Settings',
};

export function cloudNavPath(navKey: CloudNavKey, envHrid: string): string {
    if (navKey === CLOUD_SETTINGS_NAV_KEY) {
        return cloudSettingsNavPath(envHrid, 'general');
    }
    return `/environments/${envHrid}/cloud/${navKey}`;
}

export function cloudBasePath(envHrid: string): string {
    return `/environments/${envHrid}/cloud`;
}

export interface CloudBreadcrumbSegment {
    readonly label: string;
    readonly to?: string;
}

interface CloudNavArea {
    readonly navKey: CloudNavKey;
    readonly matches: (subPath: string) => boolean;
    readonly breadcrumbSegments: (envHrid: string, pathname: string) => readonly CloudBreadcrumbSegment[];
}

const CLOUD_NAV_AREAS: readonly CloudNavArea[] = [
    {
        navKey: CLOUD_SETTINGS_NAV_KEY,
        matches: sub => sub === CLOUD_SETTINGS_NAV_KEY || sub.startsWith(`${CLOUD_SETTINGS_NAV_KEY}/`),
        breadcrumbSegments: (envHrid, pathname) => {
            const settingsKey = resolveCloudSettingsNavKey(pathname, envHrid);
            return [
                { label: 'Cloud', to: cloudNavPath(CLOUD_DASHBOARD_NAV_KEY, envHrid) },
                { label: CLOUD_NAV_LABELS.settings, to: cloudSettingsNavPath(envHrid, 'general') },
                { label: CLOUD_SETTINGS_NAV_LABELS[settingsKey] },
            ];
        },
    },
    {
        navKey: CLOUD_DASHBOARD_NAV_KEY,
        matches: sub => sub === CLOUD_DASHBOARD_NAV_KEY || sub === '' || sub.startsWith(`${CLOUD_DASHBOARD_NAV_KEY}/`),
        breadcrumbSegments: envHrid => [
            { label: 'Cloud', to: cloudNavPath(CLOUD_DASHBOARD_NAV_KEY, envHrid) },
            { label: CLOUD_NAV_LABELS.dashboard },
        ],
    },
];

function extractCloudSubPath(pathname: string, envHrid: string): string | null {
    const prefix = `/environments/${envHrid}/cloud`;
    if (!pathname.startsWith(prefix)) return null;
    const tail = pathname.slice(prefix.length);
    if (tail === '' || tail === '/') return '';
    if (tail.startsWith('/')) return tail.slice(1);
    return null;
}

export function resolveCloudRoute(
    pathname: string,
    envHrid: string,
): {
    activeNavKey: CloudNavKey;
    breadcrumbSegments: readonly CloudBreadcrumbSegment[];
} {
    const defaultResult = {
        activeNavKey: CLOUD_DASHBOARD_NAV_KEY,
        breadcrumbSegments: [
            { label: 'Cloud', to: cloudNavPath(CLOUD_DASHBOARD_NAV_KEY, envHrid) },
            { label: CLOUD_NAV_LABELS.dashboard, to: cloudNavPath(CLOUD_DASHBOARD_NAV_KEY, envHrid) },
        ] as readonly CloudBreadcrumbSegment[],
    };

    const subPath = extractCloudSubPath(pathname, envHrid);
    if (subPath === null) return defaultResult;

    for (const area of CLOUD_NAV_AREAS) {
        if (area.matches(subPath)) {
            return { activeNavKey: area.navKey, breadcrumbSegments: area.breadcrumbSegments(envHrid, pathname) };
        }
    }
    return defaultResult;
}

export function isCloudNavKey(key: string): key is CloudNavKey {
    return key === CLOUD_DASHBOARD_NAV_KEY || key === CLOUD_SETTINGS_NAV_KEY;
}
