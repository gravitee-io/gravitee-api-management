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
import { availabilityFromMetric, reportAvailabilityPctFromMetric, toAvailabilityPct } from './availability';

describe('availabilityFromMetric', () => {
    it('treats a missing metric as no-data', () => {
        expect(availabilityFromMetric(undefined)).toEqual({ type: 'no-data' });
        expect(availabilityFromMetric(null)).toEqual({ type: 'no-data' });
    });

    it('treats v2 global 0 with an empty group as no-data, matching Classic', () => {
        expect(availabilityFromMetric({ global: 0, group: {} })).toEqual({ type: 'no-data' });
        expect(availabilityFromMetric({ global: 0.99, group: {} })).toEqual({ type: 'no-data' });
    });

    it('treats a populated group with missing global as no-data', () => {
        expect(availabilityFromMetric({ group: { default: 0.88 } })).toEqual({ type: 'no-data' });
        expect(availabilityFromMetric({ global: null, group: { default: 0 } })).toEqual({ type: 'no-data' });
    });

    it('converts a v2 fraction with endpoint samples into a configured percentage', () => {
        expect(availabilityFromMetric({ global: 0.88, group: { default: 0.88 } })).toEqual({
            type: 'configured',
            availabilityPct: 88,
        });
        expect(toAvailabilityPct(0.995)).toBe(99.5);
    });

    it('treats 0% as no-data, matching Classic which hides a 0 availability as "No data to display"', () => {
        expect(availabilityFromMetric({ global: 0, group: { default: 0 } })).toEqual({ type: 'no-data' });
        expect(availabilityFromMetric({ global: 0.0, group: { default: 0.0 } })).toEqual({ type: 'no-data' });
    });

    it('still shows a circle when availability is above 0', () => {
        expect(availabilityFromMetric({ global: 0.027, group: { default: 0.027 } })).toEqual({
            type: 'configured',
            availabilityPct: 2.7,
        });
    });
});

describe('reportAvailabilityPctFromMetric', () => {
    it('skips empty groups so the report does not treat missing samples as errors', () => {
        expect(reportAvailabilityPctFromMetric({ global: 0, group: {} })).toBeNull();
        expect(reportAvailabilityPctFromMetric({ global: 0.99, group: {} })).toBeNull();
        expect(reportAvailabilityPctFromMetric(undefined)).toBeNull();
    });

    it('counts a populated group at 0% as a report sample', () => {
        expect(reportAvailabilityPctFromMetric({ global: 0, group: { default: 0 } })).toBe(0);
        expect(reportAvailabilityPctFromMetric({ global: 0.0, group: { default: 0.0 } })).toBe(0);
    });

    it('keeps non-zero availability as a percentage', () => {
        expect(reportAvailabilityPctFromMetric({ global: 0.88, group: { default: 0.88 } })).toBe(88);
    });
});
