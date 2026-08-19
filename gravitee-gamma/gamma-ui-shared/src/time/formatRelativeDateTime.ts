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
const WEEK_MS = 7 * 24 * 60 * 60 * 1000;
const ABSOLUTE_DATE_TIME_OPTIONS: Intl.DateTimeFormatOptions = {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
};

function parseDate(value: number | string | undefined | null): Date | undefined {
    if (value === undefined || value === null || value === '') {
        return undefined;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return undefined;
    }
    return date;
}

/** Classic `date:'medium'` tooltip: always an absolute datetime. */
export function formatAbsoluteDateTime(value: number | string | undefined | null): string {
    const date = parseDate(value);
    if (!date) {
        return EMPTY;
    }
    return date.toLocaleString('en-GB', ABSOLUTE_DATE_TIME_OPTIONS);
}

/** Classic `humanDatetimeFilter`: relative time if younger than a week, else an absolute datetime. */
export function formatRelativeDateTime(value: number | string | undefined | null, now = Date.now()): string {
    const date = parseDate(value);
    if (!date) {
        return EMPTY;
    }
    const diffMs = now - date.getTime();
    if (diffMs < WEEK_MS) {
        return formatRelativeFromNow(diffMs);
    }
    return formatAbsoluteDateTime(value);
}

function formatRelativeFromNow(diffMs: number): string {
    const rtf = new Intl.RelativeTimeFormat('en', { numeric: 'always' });
    const seconds = Math.max(0, Math.round(diffMs / 1000));
    if (seconds < 60) {
        return rtf.format(-seconds, 'second');
    }
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) {
        return rtf.format(-minutes, 'minute');
    }
    const hours = Math.round(minutes / 60);
    if (hours < 24) {
        return rtf.format(-hours, 'hour');
    }
    return rtf.format(-Math.round(hours / 24), 'day');
}
