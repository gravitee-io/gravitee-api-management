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

/** Hardcoded KPI values for the portals dashboard POC (Active Portals is live). */
export const DUMMY_DASHBOARD_KPIS = {
    totalVisits30d: 12_847,
    apiDocsViewed30d: 5_231,
    avgUptimePercent: 99.97,
} as const;

export const TRAFFIC_SERIES_KEY = 'httpRequests';

/** Jan–Jun gateway request series for Traffic Overview (wave matching the mock). */
export const DUMMY_TRAFFIC_OVERVIEW = [
    { category: 'Jan', [TRAFFIC_SERIES_KEY]: 5200 },
    { category: 'Feb', [TRAFFIC_SERIES_KEY]: 7800 },
    { category: 'Mar', [TRAFFIC_SERIES_KEY]: 6100 },
    { category: 'Apr', [TRAFFIC_SERIES_KEY]: 3200 },
    { category: 'May', [TRAFFIC_SERIES_KEY]: 6900 },
    { category: 'Jun', [TRAFFIC_SERIES_KEY]: 5800 },
] as const;

export interface OperationalLogEntry {
    readonly id: string;
    readonly message: string;
    readonly relativeTime: string;
}

export const DUMMY_OPERATIONAL_LOG: readonly OperationalLogEntry[] = [
    {
        id: 'log-1',
        message: 'Payments API Portal deployed',
        relativeTime: '2h ago',
    },
    {
        id: 'log-2',
        message: 'New API spec published to Internal Dev Portal',
        relativeTime: '1d ago',
    },
    {
        id: 'log-3',
        message: 'SSL certificate renewed for pay.developer.acme.io',
        relativeTime: '3d ago',
    },
    {
        id: 'log-4',
        message: 'Active Fitness Partner APIs added',
        relativeTime: '3d ago',
    },
];

export type PortalSparklineTone = 'positive' | 'neutral' | 'flat';

export interface PortalDashboardStats {
    readonly requestsPerDay: number;
    readonly avgLatencyMs: number | null;
    readonly sparkline: readonly number[];
    readonly sparklineTone: PortalSparklineTone;
}

const FALLBACK_PORTAL_STATS: PortalDashboardStats = {
    requestsPerDay: 0,
    avgLatencyMs: null,
    sparkline: [0, 0, 0, 0, 0, 0, 0],
    sparklineTone: 'flat',
};

/** Per-portal POC metrics keyed by portal name (fallback for unknown portals). */
export const DUMMY_PORTAL_STATS: Readonly<Record<string, PortalDashboardStats>> = {
    'Payments API Portal': {
        requestsPerDay: 12_400,
        avgLatencyMs: 142,
        sparkline: [40, 55, 48, 62, 58, 75, 88],
        sparklineTone: 'positive',
    },
    'Internal Dev Portal': {
        requestsPerDay: 8100,
        avgLatencyMs: 89,
        sparkline: [70, 65, 72, 60, 68, 55, 62],
        sparklineTone: 'neutral',
    },
    'Active Fitness Partner APIs': {
        requestsPerDay: 3200,
        avgLatencyMs: 118,
        sparkline: [12, 18, 22, 28, 35, 42, 51],
        sparklineTone: 'positive',
    },
};

function resolvePortalStatsKey(portalName: string): string | undefined {
    if (DUMMY_PORTAL_STATS[portalName]) {
        return portalName;
    }

    const normalized = portalName.trim().toLowerCase();
    if (normalized.includes('active fitness')) {
        return 'Active Fitness Partner APIs';
    }
    if (normalized.includes('payments')) {
        return 'Payments API Portal';
    }
    if (normalized.includes('internal')) {
        return 'Internal Dev Portal';
    }
    return undefined;
}

export function getPortalDashboardStats(portalName: string): PortalDashboardStats {
    const key = resolvePortalStatsKey(portalName);
    return (key ? DUMMY_PORTAL_STATS[key] : undefined) ?? FALLBACK_PORTAL_STATS;
}
