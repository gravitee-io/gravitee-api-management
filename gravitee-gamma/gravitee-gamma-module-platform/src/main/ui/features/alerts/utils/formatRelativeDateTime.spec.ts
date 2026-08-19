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
import { formatAbsoluteDateTime, formatRelativeDateTime } from '../../../shared/time';

describe('formatRelativeDateTime', () => {
    it('formats recent timestamps as relative time from epoch millis', () => {
        const now = Date.parse('2026-08-19T06:30:00.000Z');
        expect(formatRelativeDateTime(now - 3 * 60_000, now)).toBe('3 minutes ago');
        expect(formatRelativeDateTime(now - 5_000, now)).toMatch(/second/);
        expect(formatRelativeDateTime(now - 2 * 60 * 60_000, now)).toBe('2 hours ago');
        expect(formatRelativeDateTime(undefined, now)).toBe('—');
        expect(formatRelativeDateTime('not-a-date', now)).toBe('—');
        expect(formatRelativeDateTime(now - 8 * 24 * 60 * 60_000, now)).not.toMatch(/Invalid Date|ago/);
    });
});

describe('formatAbsoluteDateTime', () => {
    it('formats epoch millis as an absolute datetime, not Invalid Date', () => {
        const createdAt = Date.parse('2026-08-19T06:27:00.000Z');
        expect(formatAbsoluteDateTime(createdAt)).not.toMatch(/Invalid Date|ago/);
        expect(formatAbsoluteDateTime(createdAt)).toMatch(/2026/);
        expect(formatAbsoluteDateTime(undefined)).toBe('—');
        expect(formatAbsoluteDateTime('not-a-date')).toBe('—');
    });
});
