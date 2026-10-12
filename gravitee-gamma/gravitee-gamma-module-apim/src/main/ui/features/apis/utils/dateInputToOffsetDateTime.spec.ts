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
import { dateInputToOffsetDateTime, offsetDateTimeToDateInput } from './dateInputToOffsetDateTime';

describe('dateInputToOffsetDateTime', () => {
    it('emits an OffsetDateTime ISO string the backend can deserialize', () => {
        expect(dateInputToOffsetDateTime('2026-10-31')).toBe('2026-10-31T12:00:00.000Z');
    });
});

describe('offsetDateTimeToDateInput', () => {
    it('uses the local calendar day, including when that day is not the UTC date', () => {
        const iso = '2026-10-19T18:30:00.000Z';
        const date = new Date(iso);
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const localDay = `${date.getFullYear()}-${month}-${day}`;

        expect(offsetDateTimeToDateInput(iso)).toBe(localDay);
        expect(offsetDateTimeToDateInput('2026-10-31T12:00:00.000Z')).toBe(localDayOf('2026-10-31T12:00:00.000Z'));
        expect(offsetDateTimeToDateInput(undefined)).toBe('');
        if (localDay !== iso.slice(0, 10)) {
            expect(offsetDateTimeToDateInput(iso)).toBe('2026-10-20');
        }
    });
});

function localDayOf(iso: string): string {
    const date = new Date(iso);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
}
