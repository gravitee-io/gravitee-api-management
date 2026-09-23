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
    availabilityFromMetric,
    availabilityPctFor,
    hasAverageSamples,
    reportAvailabilityPctFromMetric,
    toAvailabilityPct,
} from './availability';

// Shapes taken from a real v1 `/apis/{id}/health?type=availability` response.
const HEALTHY = { global: { '1m': 100.0, '1h': 100.0, '1d': 100.0, '1w': 100.0, '1M': 99.98588567395907 } } as const;
const FULLY_DOWN = { global: { '1m': 0.0, '1h': 0.0, '1d': 0.0, '1w': 0.0, '1M': 0.0 } } as const;
const NEVER_REPORTED = { global: null } as const;
/** A window average that returned buckets, which is what Classic requires before it shows the gauge. */
const WITH_SAMPLES = { values: [{ buckets: [{ name: 'default', data: [1, 1] }] }] } as const;
const NO_SAMPLES = { values: [] } as const;

describe('availabilityPctFor', () => {
    it('reads the percentage for the selected timeframe out of one payload', () => {
        expect(availabilityPctFor(HEALTHY, '1m')).toBe(100);
        expect(availabilityPctFor(HEALTHY, '1M')).toBe(99.99);
    });

    it('tells an API that never reported apart from one reporting 0%', () => {
        expect(availabilityPctFor(NEVER_REPORTED, '1m')).toBeNull();
        expect(availabilityPctFor(undefined, '1m')).toBeNull();
        expect(availabilityPctFor(null, '1m')).toBeNull();
        expect(availabilityPctFor(FULLY_DOWN, '1m')).toBe(0);
    });

    it('returns null for a timeframe the payload does not carry', () => {
        expect(availabilityPctFor({ global: { '1h': 50 } }, '1m')).toBeNull();
    });

    it('rounds to the two decimals Classic shows', () => {
        expect(toAvailabilityPct(99.98588567395907)).toBe(99.99);
        expect(toAvailabilityPct(2.7)).toBe(2.7);
    });
});

describe('availabilityFromMetric', () => {
    it('treats a missing metric as no-data', () => {
        expect(availabilityFromMetric(undefined, WITH_SAMPLES, '1m')).toEqual({ type: 'no-data' });
        expect(availabilityFromMetric(null, WITH_SAMPLES, '1m')).toEqual({ type: 'no-data' });
        expect(availabilityFromMetric(NEVER_REPORTED, WITH_SAMPLES, '1m')).toEqual({ type: 'no-data' });
    });

    it('shows the gauge for the selected timeframe', () => {
        expect(availabilityFromMetric(HEALTHY, WITH_SAMPLES, '1m')).toEqual({ type: 'configured', availabilityPct: 100 });
        expect(availabilityFromMetric(HEALTHY, WITH_SAMPLES, '1M')).toEqual({ type: 'configured', availabilityPct: 99.99 });
    });

    it('hides a true 0% as no-data, matching Classic', () => {
        expect(availabilityFromMetric(FULLY_DOWN, WITH_SAMPLES, '1m')).toEqual({ type: 'no-data' });
    });

    it('still shows a circle when availability is just above 0', () => {
        expect(availabilityFromMetric({ global: { '1m': 2.7 } }, WITH_SAMPLES, '1m')).toEqual({ type: 'configured', availabilityPct: 2.7 });
    });
});

describe('hasAverageSamples', () => {
    it("is true only when the window returned buckets, like Classic's has() check", () => {
        expect(hasAverageSamples(WITH_SAMPLES)).toBe(true);
        expect(hasAverageSamples(NO_SAMPLES)).toBe(false);
        expect(hasAverageSamples({ values: [{ buckets: [] }] })).toBe(false);
        expect(hasAverageSamples(undefined)).toBe(false);
    });

    it('hides an API whose window has no samples even when its lifetime percentage is high', () => {
        expect(availabilityFromMetric(HEALTHY, NO_SAMPLES, '1m')).toEqual({ type: 'no-data' });
    });
});

describe('reportAvailabilityPctFromMetric', () => {
    it('skips an API that never reported, so the report does not treat silence as an error', () => {
        expect(reportAvailabilityPctFromMetric(NEVER_REPORTED, '1m')).toBeNull();
        expect(reportAvailabilityPctFromMetric(undefined, '1m')).toBeNull();
    });

    it('counts a fully-down API at 0%, so it lands in the error bucket', () => {
        expect(reportAvailabilityPctFromMetric(FULLY_DOWN, '1m')).toBe(0);
    });

    it('keeps a reported percentage as-is', () => {
        expect(reportAvailabilityPctFromMetric(HEALTHY, '1m')).toBe(100);
    });
});
