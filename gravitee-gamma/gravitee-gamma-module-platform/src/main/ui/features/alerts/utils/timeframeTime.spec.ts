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
import {
    END_OF_DAY_SECONDS,
    OFFICE_END_SECONDS,
    OFFICE_START_SECONDS,
    isOfficeHours,
    secondsSinceMidnightToTimeInput,
    timeInputToSecondsSinceMidnight,
} from './timeframeTime';

describe('timeframeTime', () => {
    it('formats seconds since midnight as HH:mm:ss', () => {
        expect(secondsSinceMidnightToTimeInput(0)).toBe('00:00:00');
        expect(secondsSinceMidnightToTimeInput(OFFICE_START_SECONDS)).toBe('09:00:00');
        expect(secondsSinceMidnightToTimeInput(OFFICE_END_SECONDS)).toBe('18:00:00');
        expect(secondsSinceMidnightToTimeInput(END_OF_DAY_SECONDS)).toBe('23:59:59');
        expect(secondsSinceMidnightToTimeInput(9 * 3600 + 15 * 60 + 30)).toBe('09:15:30');
    });

    it('parses HH:mm:ss (and HH:mm) into seconds since midnight', () => {
        expect(timeInputToSecondsSinceMidnight('09:00:00')).toBe(OFFICE_START_SECONDS);
        expect(timeInputToSecondsSinceMidnight('18:00:00')).toBe(OFFICE_END_SECONDS);
        expect(timeInputToSecondsSinceMidnight('09:15:30')).toBe(9 * 3600 + 15 * 60 + 30);
        expect(timeInputToSecondsSinceMidnight('09:00')).toBe(OFFICE_START_SECONDS);
    });

    it('treats 09:00:00–18:00:00 as office hours', () => {
        expect(isOfficeHours(OFFICE_START_SECONDS, OFFICE_END_SECONDS)).toBe(true);
        expect(isOfficeHours(0, END_OF_DAY_SECONDS)).toBe(false);
        expect(isOfficeHours(9, 18)).toBe(false);
    });
});
