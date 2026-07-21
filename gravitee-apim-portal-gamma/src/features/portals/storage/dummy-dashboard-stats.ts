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
