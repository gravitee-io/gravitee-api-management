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
import type { ApiAvailabilityMetric, ApiHealthAverage } from '../types';
import type { Timeframe } from './healthTimeframe';

export type AvailabilityView = { readonly type: 'no-data' } | { readonly type: 'configured'; readonly availabilityPct: number };

/** Rounds a v1 availability percentage to two decimals, the precision Classic's gauge shows. */
export function toAvailabilityPct(pct: number): number {
    return Math.round(pct * 100) / 100;
}

/**
 * The percentage the API reported for this timeframe, or null when it has never reported at all.
 * A reporting API that is fully down returns 0, which is a number, not null.
 */
export function availabilityPctFor(metric: ApiAvailabilityMetric | null | undefined, timeframe: Timeframe): number | null {
    const pct = metric?.global?.[timeframe];
    return typeof pct === 'number' && !Number.isNaN(pct) ? toAvailabilityPct(pct) : null;
}

/**
 * Table cell, matching Classic's rule exactly: the gauge appears only when the timeframe has a non-zero
 * percentage AND the window average actually returned buckets
 * (`has(healthAvailabilityTimeFrame, 'values[0].buckets[0].data')`). Classic hides a true 0% behind
 * "No data to display", and hides an API whose window carries no samples even if its lifetime percentage is high.
 */
export function hasAverageSamples(average: ApiHealthAverage | null | undefined): boolean {
    return Array.isArray(average?.values?.[0]?.buckets?.[0]?.data);
}

export function availabilityFromMetric(
    metric: ApiAvailabilityMetric | null | undefined,
    average: ApiHealthAverage | null | undefined,
    timeframe: Timeframe,
): AvailabilityView {
    const availabilityPct = availabilityPctFor(metric, timeframe);
    if (availabilityPct === null || availabilityPct === 0 || !hasAverageSamples(average)) {
        return { type: 'no-data' };
    }
    return { type: 'configured', availabilityPct };
}

/** Report sample: an API reporting 0% is counted, so a fully-down API still lands in the error bucket. */
export function reportAvailabilityPctFromMetric(metric: ApiAvailabilityMetric | null | undefined, timeframe: Timeframe): number | null {
    return availabilityPctFor(metric, timeframe);
}
