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
import type {
    ApiAvailability,
    ApiAverageResponseTime,
    ApiHealthFieldMetric,
    ApiHealthResponseTimeOvertime,
    AvailabilityRow,
    HealthGlobalStats,
    ResponseTimeTrendPoint,
} from '../types/healthCheck';

/** Availability thresholds (percentage) reused from the legacy dashboard. */
export const AVAILABILITY_ERROR_THRESHOLD = 80;
export const AVAILABILITY_WARNING_THRESHOLD = 95;

/**
 * The backend types `group` as either a keyed object or an array of single-key
 * objects. This normalizes both into a flat `Record<string, number>`.
 */
export function normalizeGroup(group: ApiHealthFieldMetric['group'] | undefined): Record<string, number> {
    if (!group) return {};
    if (Array.isArray(group)) {
        return group.reduce<Record<string, number>>((acc, entry) => Object.assign(acc, entry), {});
    }
    return group;
}

/** Converts an availability fraction in [0..1] to a rounded percentage in [0..100]. */
export function toAvailabilityPct(fraction: number | undefined): number {
    if (fraction === undefined || fraction === null || Number.isNaN(fraction)) return 0;
    return Math.round(fraction * 100 * 100) / 100;
}

export function toGlobalStats(
    availability: ApiAvailability | undefined,
    responseTime: ApiAverageResponseTime | undefined,
): HealthGlobalStats {
    return {
        availabilityPct: toAvailabilityPct(availability?.global),
        avgResponseTimeMs: Math.round(responseTime?.global ?? 0),
    };
}

/**
 * Merges per-field availability and average-response-time responses into table
 * rows keyed by endpoint/gateway name. Availability drives the row set; response
 * time is matched by key when present.
 */
export function toAvailabilityRows(
    availability: ApiAvailability | undefined,
    responseTime: ApiAverageResponseTime | undefined,
): AvailabilityRow[] {
    const availabilityByKey = normalizeGroup(availability?.group);
    const responseTimeByKey = normalizeGroup(responseTime?.group);

    return Object.entries(availabilityByKey).map(([key, fraction]) => ({
        key,
        name: key,
        availabilityPct: toAvailabilityPct(fraction),
        avgResponseTimeMs: key in responseTimeByKey ? Math.round(responseTimeByKey[key]) : undefined,
    }));
}

/** Maps the over-time response into chart points with absolute bucket timestamps. */
export function toResponseTimeTrend(overtime: ApiHealthResponseTimeOvertime | undefined): ResponseTimeTrendPoint[] {
    if (!overtime?.data?.length) return [];
    const { from, interval } = overtime.timeRange;
    return overtime.data.map((responseTime, index) => ({
        timestamp: from + index * interval,
        responseTime: Math.round(responseTime),
    }));
}

/** Formats an ISO timestamp string for display; falls back to the raw string on parse failure. */
export function formatTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    return Number.isNaN(date.getTime()) ? timestamp : date.toLocaleString();
}

export type AvailabilitySeverity = 'error' | 'warning' | 'success';

/** Severity bucket for an availability percentage, reusing legacy 80/95 thresholds. */
export function availabilitySeverity(pct: number): AvailabilitySeverity {
    if (pct < AVAILABILITY_ERROR_THRESHOLD) return 'error';
    if (pct <= AVAILABILITY_WARNING_THRESHOLD) return 'warning';
    return 'success';
}

const SEVERITY_TEXT_CLASS: Record<AvailabilitySeverity, string> = {
    error: 'text-destructive',
    warning: 'text-warning',
    success: 'text-success',
};

/** Tailwind text-color class for an availability percentage. */
export function availabilityColorClass(pct: number): string {
    return SEVERITY_TEXT_CLASS[availabilitySeverity(pct)];
}
