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

import { isValidIso8601Duration, parseIso8601DurationSeconds } from './iso8601Duration';

describe('iso8601Duration', () => {
    it('parses second-based durations', () => {
        expect(parseIso8601DurationSeconds('PT1S')).toBe(1);
        expect(parseIso8601DurationSeconds('PT5S')).toBe(5);
        expect(parseIso8601DurationSeconds('PT15S')).toBe(15);
    });

    it('parses minute-based durations', () => {
        expect(parseIso8601DurationSeconds('PT1M')).toBe(60);
    });

    it('parses day-based durations', () => {
        expect(parseIso8601DurationSeconds('P1D')).toBe(86_400);
    });

    it('rejects invalid or sub-second durations', () => {
        expect(parseIso8601DurationSeconds('')).toBeNull();
        expect(parseIso8601DurationSeconds('PT0.5S')).toBeNull();
        expect(parseIso8601DurationSeconds('not-a-duration')).toBeNull();
        expect(isValidIso8601Duration('PT0S')).toBe(false);
    });

    it('rejects year, month, and week durations unsupported by java.time.Duration', () => {
        expect(parseIso8601DurationSeconds('P1Y')).toBeNull();
        expect(parseIso8601DurationSeconds('P1M')).toBeNull();
        expect(parseIso8601DurationSeconds('P1W')).toBeNull();
        expect(isValidIso8601Duration('P1Y')).toBe(false);
        expect(isValidIso8601Duration('P1M')).toBe(false);
        expect(isValidIso8601Duration('P1W')).toBe(false);
    });
});
