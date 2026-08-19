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

/** Classic `TimeframeRanges` for env alert activity (interval unused — analytics is from/to only). */
export const ALERT_ACTIVITY_TIME_RANGES = [
    { id: '1m', title: 'Last minute', rangeMs: 1000 * 60 },
    { id: '1h', title: 'Last hour', rangeMs: 1000 * 60 * 60 },
    { id: '1d', title: 'Last day', rangeMs: 1000 * 60 * 60 * 24 },
    { id: '1w', title: 'Last week', rangeMs: 1000 * 60 * 60 * 24 * 7 },
    { id: '1M', title: 'Last month', rangeMs: 1000 * 60 * 60 * 24 * 30 },
] as const;

export type AlertActivityTimeRangeId = (typeof ALERT_ACTIVITY_TIME_RANGES)[number]['id'];

export const DEFAULT_ALERT_ACTIVITY_TIME_RANGE_ID: AlertActivityTimeRangeId = '1m';

export function getAlertActivityTimeRange(id: AlertActivityTimeRangeId) {
    return ALERT_ACTIVITY_TIME_RANGES.find(range => range.id === id) ?? ALERT_ACTIVITY_TIME_RANGES[0];
}

export function activityTimeWindow(rangeMs: number, now = Date.now()): { from: number; to: number } {
    return { from: now - rangeMs, to: now };
}
