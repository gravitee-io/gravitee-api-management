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

import { formatInstanceDate } from './formatInstanceDate';

describe('formatInstanceDate', () => {
    it('returns an em dash for empty values', () => {
        expect(formatInstanceDate(undefined)).toBe('—');
        expect(formatInstanceDate(null)).toBe('—');
        expect(formatInstanceDate('')).toBe('—');
    });

    it('returns an em dash for invalid dates', () => {
        expect(formatInstanceDate('not-a-date')).toBe('—');
    });

    it('formats timestamps with en-US medium-like style', () => {
        const formatted = formatInstanceDate(Date.UTC(2026, 4, 7, 9, 43, 26));
        expect(formatted).toContain('2026');
        expect(formatted).toContain('7');
        expect(formatted).not.toBe('—');
    });
});
