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
import { nextAlertPositiveNumber } from './alertPositiveNumber';

describe('nextAlertPositiveNumber', () => {
    it('clears when the field is emptied', () => {
        expect(nextAlertPositiveNumber('', 10)).toBeUndefined();
    });

    it('accepts values of 1 or more', () => {
        expect(nextAlertPositiveNumber('1', undefined)).toBe(1);
        expect(nextAlertPositiveNumber('500', 10)).toBe(500);
    });

    it('keeps the current value for negatives, zero, and non-numeric input (classic min=1)', () => {
        expect(nextAlertPositiveNumber('-8', 10)).toBe(10);
        expect(nextAlertPositiveNumber('0', 10)).toBe(10);
        expect(nextAlertPositiveNumber('-', 10)).toBe(10);
    });

    it('rejects values above max when a max is set (classic rate threshold 1–99)', () => {
        expect(nextAlertPositiveNumber('50', 10, { max: 99 })).toBe(50);
        expect(nextAlertPositiveNumber('100', 50, { max: 99 })).toBe(50);
    });

    it('rejects values below a custom min (classic high threshold >= low)', () => {
        expect(nextAlertPositiveNumber('150', 500, { min: 200 })).toBe(500);
        expect(nextAlertPositiveNumber('250', 500, { min: 200 })).toBe(250);
    });
});
