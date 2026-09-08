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

export const AVAILABILITY_ERROR_THRESHOLD = 80;
export const AVAILABILITY_WARNING_THRESHOLD = 95;

export type AvailabilityBucket = 'operational' | 'warning' | 'error';

export interface HealthCheckReport {
    readonly operational: number;
    readonly inWarning: number;
    readonly inError: number;
}

export function bucketAvailability(pct: number | null | undefined): AvailabilityBucket | null {
    if (pct === null || pct === undefined || Number.isNaN(pct)) {
        return null;
    }
    if (pct <= AVAILABILITY_ERROR_THRESHOLD) {
        return 'error';
    }
    if (pct <= AVAILABILITY_WARNING_THRESHOLD) {
        return 'warning';
    }
    return 'operational';
}

export function summarizeReportBuckets(samples: ReadonlyArray<number | null | undefined>): HealthCheckReport {
    return samples.reduce<HealthCheckReport>(
        (acc, sample) => {
            const bucket = bucketAvailability(sample);
            if (bucket === 'error') {
                return { ...acc, inError: acc.inError + 1 };
            }
            if (bucket === 'warning') {
                return { ...acc, inWarning: acc.inWarning + 1 };
            }
            if (bucket === 'operational') {
                return { ...acc, operational: acc.operational + 1 };
            }
            return acc;
        },
        { operational: 0, inWarning: 0, inError: 0 },
    );
}
