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

const EMPTY = '—';

/** Formats classic alert event counters as `5m / 1h / 1d / 1M`. */
export function formatAlertCounters(counters: Record<string, number> | undefined): string {
    if (!counters) {
        return EMPTY;
    }
    return `${counters['5m'] ?? 0} / ${counters['1h'] ?? 0} / ${counters['1d'] ?? 0} / ${counters['1M'] ?? 0}`;
}

export function formatAlertCountersTooltip(counters: Record<string, number> | undefined): string | undefined {
    if (!counters) {
        return undefined;
    }
    return (
        `${counters['5m'] ?? 0} during the last 5 minutes / ` +
        `${counters['1h'] ?? 0} during the last 1 hour / ` +
        `${counters['1d'] ?? 0} during the last 1 day / ` +
        `${counters['1M'] ?? 0} during the last 1 month`
    );
}

/** Formats `last_alert_at` for the list (classic medium datetime). */
export function formatLastAlertAt(value: string | null | undefined): string {
    if (value === undefined || value === null || value === '') {
        return EMPTY;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return EMPTY;
    }
    return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        second: '2-digit',
    });
}

export function formatLastAlertMessage(message: string | null | undefined): string {
    if (message === undefined || message === null || message === '') {
        return EMPTY;
    }
    return message;
}
