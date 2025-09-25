/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { FormatNumberPipe } from './format-number.pipe';

describe('FormatNumberPipe', () => {
  const pipe = new FormatNumberPipe();

  describe('transform', () => {
    const testCases = [
      // --- Invalid and Edge Case Inputs ---
      { input: null, expected: 'N/A' },
      { input: undefined, expected: 'N/A' },
      { input: NaN, expected: 'N/A' },
      { input: 0, expected: '0' },

      // --- Numbers Less Than 1000 ---
      { input: 123, expected: '123' },
      { input: 999, expected: '999' },
      { input: -123, expected: '-123' },
      { input: -999, expected: '-999' },

      // --- Thousands (K) ---
      { input: 1000, expected: '1K' },
      { input: 1234, expected: '1.2K' },
      { input: 9876, expected: '9.9K' },
      { input: 1990, expected: '2K' },

      // --- Millions (M) ---
      { input: 1_000_000, expected: '1M' },
      { input: 1_550_000, expected: '1.6M' },
      { input: 123_456_789, expected: '123.5M' },

      // --- Billions (B) ---
      { input: 1_000_000_000, expected: '1B' },
      { input: 2_345_000_000, expected: '2.3B' },
    ];

    // A single parameterized test runs all the cases from the table above
    it.each(testCases)('should transform $input to "$expected"', ({ input, expected }) => {
      expect(pipe.transform(input)).toBe(expected);
    });
  });
});
