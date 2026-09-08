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
import type { ApiAvailabilityMetric } from '../types';

export type AvailabilityView = { readonly type: 'no-data' } | { readonly type: 'configured'; readonly availabilityPct: number };

/** Converts an availability fraction in [0..1] to a rounded percentage in [0..100]. */
export function toAvailabilityPct(fraction: number | undefined): number {
    if (fraction === undefined || fraction === null || Number.isNaN(fraction)) {
        return 0;
    }
    return Math.round(fraction * 100 * 100) / 100;
}

function hasEndpointSamples(group: unknown): boolean {
    return group !== null && typeof group === 'object' && !Array.isArray(group) && Object.keys(group).length > 0;
}

/**
 * v2 GET /health/availability returns `{ global: 0.0, group: {} }` when the window
 * has no samples. Classic's table also hides a true 0% (`!global.1m` is true for 0)
 * as "No data to display". A circle is shown only when availability is above 0.
 */
export function availabilityFromMetric(metric: ApiAvailabilityMetric | null | undefined): AvailabilityView {
    const availabilityPct = reportAvailabilityPctFromMetric(metric);
    if (availabilityPct === null || availabilityPct === 0) {
        return { type: 'no-data' };
    }
    return { type: 'configured', availabilityPct };
}

/** Report sample: populated group at 0% is counted; empty group is skipped. */
export function reportAvailabilityPctFromMetric(metric: ApiAvailabilityMetric | null | undefined): number | null {
    if (!metric || !hasEndpointSamples(metric.group)) {
        return null;
    }
    if (metric.global === undefined || metric.global === null || Number.isNaN(metric.global)) {
        return null;
    }
    return toAvailabilityPct(metric.global);
}
