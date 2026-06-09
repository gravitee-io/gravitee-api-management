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

/** Selectable health-check time windows (mirrors the legacy V4 dashboard). */
export type Timeframe = '1m' | '1h' | '1d' | '1w' | '1M';

export interface HealthTimeRange {
    /** Window start (epoch ms). */
    from: number;
    /** Window end (epoch ms). */
    to: number;
    /** Bucket size (ms) for over-time series. */
    interval: number;
}

const MINUTE = 1000 * 60;
const HOUR = MINUTE * 60;
const DAY = HOUR * 24;

export const TIMEFRAME_DURATIONS_MS: Record<Timeframe, number> = {
    '1m': MINUTE,
    '1h': HOUR,
    '1d': DAY,
    '1w': DAY * 7,
    '1M': DAY * 30,
};

export const DEFAULT_TIMEFRAME: Timeframe = '1d';

/** Number of buckets the over-time series is split into. */
export const DEFAULT_BUCKET_COUNT = 30;

export interface TimeframeOption {
    id: Timeframe;
    label: string;
}

export const TIMEFRAMES: TimeframeOption[] = [
    { id: '1m', label: 'Last minute' },
    { id: '1h', label: 'Last hour' },
    { id: '1d', label: 'Last day' },
    { id: '1w', label: 'Last week' },
    { id: '1M', label: 'Last month' },
];

/**
 * Resolves a timeframe id to an absolute [from, to] window and a bucket interval.
 * `now` is injectable for deterministic tests.
 */
export function resolveHealthTimeRange(
    timeframe: Timeframe,
    nbBuckets: number = DEFAULT_BUCKET_COUNT,
    now: number = Date.now(),
): HealthTimeRange {
    const duration = TIMEFRAME_DURATIONS_MS[timeframe];
    return {
        from: now - duration,
        to: now,
        interval: Math.floor(duration / nbBuckets),
    };
}
