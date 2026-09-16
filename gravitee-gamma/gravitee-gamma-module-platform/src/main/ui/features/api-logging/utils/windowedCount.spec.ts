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

import { WindowedCount, WindowedCountFormatError } from './windowedCount';

describe('WindowedCount', () => {
    it('parses count/duration values', () => {
        const parsed = WindowedCount.parse('2/PT15S');
        expect(parsed.count).toBe(2);
        expect(parsed.windowSeconds).toBe(15);
        expect(parsed.rate()).toBeCloseTo(2 / 15);
    });

    it('rejects invalid formats', () => {
        expect(() => WindowedCount.parse('invalid')).toThrow(WindowedCountFormatError);
        expect(() => WindowedCount.parse('0/PT1S')).toThrow(WindowedCountFormatError);
    });

    it('rejects partial integer counts', () => {
        expect(() => WindowedCount.parse('2abc/PT1S')).toThrow(WindowedCountFormatError);
        expect(() => WindowedCount.parse('2.5/PT1S')).toThrow(WindowedCountFormatError);
    });

    it('returns zero rate when the window duration is zero', () => {
        expect(new WindowedCount(5, 0).rate()).toBe(0);
    });
});
