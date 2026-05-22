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
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { parseUpdatedAt, relativeTime } from '../PolicyListTable';

describe('parseUpdatedAt', () => {
    it('treats a small number as UNIX seconds (bug E: backend sends seconds float)', () => {
        // 1777283625.541 = 2026-04-27T... in seconds → must NOT become 1970.
        const date = parseUpdatedAt(1777283625.541);
        expect(date.getUTCFullYear()).toBeGreaterThan(2020);
        expect(date.getTime()).toBe(Math.round(1777283625.541 * 1000));
    });

    it('treats a large number as already-milliseconds', () => {
        const ms = Date.UTC(2026, 3, 27, 12, 0, 0); // > 1e12
        const date = parseUpdatedAt(ms);
        expect(date.getTime()).toBe(ms);
    });

    it('treats a numeric string like "1777283625.541" as UNIX seconds', () => {
        const date = parseUpdatedAt('1777283625.541');
        expect(date.getUTCFullYear()).toBeGreaterThan(2020);
    });

    it('parses an ISO string as-is', () => {
        const iso = '2026-04-27T10:00:00.000Z';
        const date = parseUpdatedAt(iso);
        expect(date.toISOString()).toBe(iso);
    });

    it('returns an invalid date for non-numeric, non-string inputs', () => {
        expect(Number.isNaN(parseUpdatedAt(undefined).getTime())).toBe(true);
        expect(Number.isNaN(parseUpdatedAt(null).getTime())).toBe(true);
        expect(Number.isNaN(parseUpdatedAt({}).getTime())).toBe(true);
    });
});

describe('relativeTime', () => {
    beforeEach(() => {
        // Freeze "now" so the relative buckets are deterministic.
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-04-27T10:00:00.000Z'));
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('returns "Just now" for a UNIX-seconds value within the last minute', () => {
        const tenSecondsAgoSeconds = Date.now() / 1000 - 10;
        expect(relativeTime(tenSecondsAgoSeconds)).toBe('Just now');
    });

    it('returns "X min ago" for a UNIX-seconds value 5 minutes old', () => {
        const fiveMinAgoSeconds = Date.now() / 1000 - 5 * 60;
        expect(relativeTime(fiveMinAgoSeconds)).toBe('5 min ago');
    });

    it('returns "Xh ago" for a 3-hour-old UNIX-seconds value', () => {
        const threeHoursAgoSeconds = Date.now() / 1000 - 3 * 3600;
        expect(relativeTime(threeHoursAgoSeconds)).toBe('3h ago');
    });

    it('does NOT report 1970 for a backend-style UNIX-seconds timestamp (regression: bug E)', () => {
        const result = relativeTime(1777283625.541);
        expect(result).not.toMatch(/1970/);
    });

    it('falls back to em-dash for invalid inputs rather than rendering "Invalid Date"', () => {
        expect(relativeTime(undefined)).toBe('—');
        expect(relativeTime('not a date')).toBe('—');
    });
});
