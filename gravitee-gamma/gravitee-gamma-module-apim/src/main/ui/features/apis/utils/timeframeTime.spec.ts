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
import { END_OF_DAY_SECONDS, secondsSinceMidnightToTimeInput, timeInputToSecondsSinceMidnight } from './timeframeTime';

describe('timeframeTime', () => {
    it('round-trips HH:MM:SS through seconds since midnight', () => {
        const input = '22:30:00';
        const seconds = timeInputToSecondsSinceMidnight(input);
        expect(seconds).toBe(81000);
        expect(secondsSinceMidnightToTimeInput(seconds)).toBe(input);
    });

    it('clamps values above end of day', () => {
        expect(timeInputToSecondsSinceMidnight('25:00:00')).toBe(END_OF_DAY_SECONDS);
    });

    it('clamps negative parts to zero', () => {
        expect(timeInputToSecondsSinceMidnight('-1:00:00')).toBe(0);
    });

    it('returns zero for malformed input', () => {
        expect(timeInputToSecondsSinceMidnight('not-a-time')).toBe(0);
    });
});
