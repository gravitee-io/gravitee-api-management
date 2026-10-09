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
    it('keeps only the calendar date for the date input', () => {
        expect(offsetDateTimeToDateInput('2026-10-31T12:00:00.000Z')).toBe('2026-10-31');
        expect(offsetDateTimeToDateInput(undefined)).toBe('');
    });
});
