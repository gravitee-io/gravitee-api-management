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

import { humanizeSize, humanizeUptime, ratio, ratioLabel } from './monitoringFormatters';

describe('monitoringFormatters', () => {
    describe('humanizeSize', () => {
        it.each([
            [Infinity, 1, '-'],
            [42, 1, '42.0 bytes'],
            [1050, 1, '1.0 kB'],
            [100056, 1, '97.7 kB'],
            [4096000000, 2, '3.81 GB'],
            [0, 1, '0.0 bytes'],
            [42, undefined, '42.0 bytes'],
            [42, 0, '42 bytes'],
        ])('converts %p bytes with precision %p into %p', (bytes, precision, result) => {
            expect(humanizeSize(bytes, precision)).toBe(result);
        });
    });

    describe('ratio', () => {
        it.each([
            [42, 1, 4200],
            [42, 0, undefined],
            [0, 42, 0],
            [115312310, 240, 48046796],
            [240, 115312310, 0],
        ])('computes ratio of %p on %p into %p', (val1, val2, result) => {
            expect(ratio(val1, val2)).toBe(result);
        });
    });

    describe('ratioLabel', () => {
        it.each([
            [42, 1, '4200%'],
            [42, 0, '-%'],
            [0, 42, '0%'],
            [115312310, 240, '48046796%'],
            [240, 115312310, '0%'],
        ])('computes ratio label of %p on %p into %p', (val1, val2, result) => {
            expect(ratioLabel(val1, val2)).toBe(result);
        });
    });

    describe('humanizeUptime', () => {
        it('returns a dash for non-finite values', () => {
            expect(humanizeUptime(Number.NaN)).toBe('-');
            expect(humanizeUptime(Number.POSITIVE_INFINITY)).toBe('-');
        });

        it('returns a few seconds for sub-minute uptime', () => {
            expect(humanizeUptime(45_000)).toBe('a few seconds');
        });

        it('formats minutes', () => {
            expect(humanizeUptime(5 * 60 * 1_000)).toBe('5 minutes');
            expect(humanizeUptime(1 * 60 * 1_000)).toBe('1 minute');
        });

        it('formats hours and minutes', () => {
            expect(humanizeUptime(3 * 60 * 60 * 1_000 + 15 * 60 * 1_000)).toBe('3 hours 15 minutes');
            expect(humanizeUptime(1 * 60 * 60 * 1_000)).toBe('1 hour');
        });

        it('formats days and hours', () => {
            expect(humanizeUptime(2 * 24 * 60 * 60 * 1_000 + 4 * 60 * 60 * 1_000)).toBe('2 days 4 hours');
            expect(humanizeUptime(1 * 24 * 60 * 60 * 1_000)).toBe('1 day');
        });
    });
});
