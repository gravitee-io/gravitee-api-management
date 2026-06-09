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
import {
    availabilityColorClass,
    availabilitySeverity,
    normalizeGroup,
    toAvailabilityPct,
    toAvailabilityRows,
    toGlobalStats,
    toResponseTimeTrend,
} from './healthCheckDashboard';

describe('normalizeGroup', () => {
    it('returns an empty object for undefined', () => {
        expect(normalizeGroup(undefined)).toEqual({});
    });

    it('passes through a keyed object', () => {
        expect(normalizeGroup({ a: 1, b: 0.5 })).toEqual({ a: 1, b: 0.5 });
    });

    it('flattens an array of single-key objects', () => {
        expect(normalizeGroup([{ a: 1 }, { b: 0.5 }])).toEqual({ a: 1, b: 0.5 });
    });
});

describe('toAvailabilityPct', () => {
    it('converts a [0..1] fraction to a percentage', () => {
        expect(toAvailabilityPct(0.9268)).toBe(92.68);
        expect(toAvailabilityPct(1)).toBe(100);
    });

    it('defaults missing or NaN values to 0', () => {
        expect(toAvailabilityPct(undefined)).toBe(0);
        expect(toAvailabilityPct(Number.NaN)).toBe(0);
    });
});

describe('toGlobalStats', () => {
    it('builds availability % and rounded avg response time', () => {
        const stats = toGlobalStats({ global: 0.95, group: {} }, { global: 87.4, group: {} });
        expect(stats).toEqual({ availabilityPct: 95, avgResponseTimeMs: 87 });
    });

    it('handles missing inputs', () => {
        expect(toGlobalStats(undefined, undefined)).toEqual({ availabilityPct: 0, avgResponseTimeMs: 0 });
    });
});

describe('toAvailabilityRows', () => {
    it('merges availability and response time by key', () => {
        const rows = toAvailabilityRows(
            { global: 0.9, group: { 'endpoint-1': 0.99, 'endpoint-2': 0.7 } },
            { global: 50, group: { 'endpoint-1': 42, 'endpoint-2': 120 } },
        );
        expect(rows).toEqual([
            { key: 'endpoint-1', name: 'endpoint-1', availabilityPct: 99, avgResponseTimeMs: 42 },
            { key: 'endpoint-2', name: 'endpoint-2', availabilityPct: 70, avgResponseTimeMs: 120 },
        ]);
    });

    it('leaves response time undefined when no matching key', () => {
        const rows = toAvailabilityRows({ global: 1, group: { ep: 1 } }, { global: 0, group: {} });
        expect(rows[0].avgResponseTimeMs).toBeUndefined();
    });

    it('supports the legacy array group shape', () => {
        const rows = toAvailabilityRows({ global: 1, group: [{ ep: 1 }] }, { global: 0, group: [{ ep: 10 }] });
        expect(rows).toEqual([{ key: 'ep', name: 'ep', availabilityPct: 100, avgResponseTimeMs: 10 }]);
    });
});

describe('toResponseTimeTrend', () => {
    it('maps buckets to absolute timestamps', () => {
        const trend = toResponseTimeTrend({ timeRange: { from: 1000, to: 4000, interval: 1000 }, data: [10.4, 20.6, 30] });
        expect(trend).toEqual([
            { timestamp: 1000, responseTime: 10 },
            { timestamp: 2000, responseTime: 21 },
            { timestamp: 3000, responseTime: 30 },
        ]);
    });

    it('returns an empty array when there is no data', () => {
        expect(toResponseTimeTrend(undefined)).toEqual([]);
        expect(toResponseTimeTrend({ timeRange: { from: 0, to: 0, interval: 0 }, data: [] })).toEqual([]);
    });
});

describe('availability severity + color', () => {
    it('classifies by the 80/95 thresholds', () => {
        // below 80 → error; exactly 80 → warning (boundary belongs to warning bucket)
        expect(availabilitySeverity(79.99)).toBe('error');
        expect(availabilitySeverity(80)).toBe('warning');
        expect(availabilitySeverity(90)).toBe('warning');
        // exactly 95 → warning; above 95 → success
        expect(availabilitySeverity(95)).toBe('warning');
        expect(availabilitySeverity(99)).toBe('success');
    });

    it('maps severity to tailwind text classes', () => {
        expect(availabilityColorClass(50)).toBe('text-destructive');
        expect(availabilityColorClass(90)).toBe('text-warning');
        expect(availabilityColorClass(100)).toBe('text-success');
    });
});
