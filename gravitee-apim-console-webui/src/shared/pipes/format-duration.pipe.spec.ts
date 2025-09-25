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

import { FormatDurationPipe } from './format-duration.pipe';

describe('FormatDurationPipe', () => {
  let pipe: FormatDurationPipe;

  beforeEach(() => {
    pipe = new FormatDurationPipe();
  });

  // Test suite for invalid and edge case inputs
  describe('Invalid and Edge Case Inputs', () => {
    const testCases = [
      { input: null, expected: 'N/A', description: 'a null input' },
      { input: undefined, expected: 'N/A', description: 'an undefined input' },
      { input: NaN, expected: 'N/A', description: 'a NaN input' },
      { input: 0, expected: '0ms', description: 'an input of 0' },
    ];

    it.each(testCases)('should return "$expected" for $description', ({ input, expected }) => {
      expect(pipe.transform(input)).toBe(expected);
    });
  });

  // Test suite for all valid duration formatting
  describe('Duration Formatting', () => {
    const testCases = [
      // Milliseconds
      { input: 500, expected: '500ms' },
      { input: 999, expected: '999ms' },
      // Seconds
      { input: 1000, expected: '1s' },
      { input: 1500, expected: '1.5s' },
      { input: 59999, expected: '60s' },
      // Minutes
      { input: 60000, expected: '1min' },
      { input: 90000, expected: '1.5min' },
      { input: 3596400, expected: '59.9min' },
      { input: 3599999, expected: '60min' },
      // Hours
      { input: 3600000, expected: '1h' },
      { input: 5400000, expected: '1.5h' },
      { input: 86040011, expected: '23.9h' },
      { input: 86399999, expected: '24h' },
      // Days
      { input: 86400000, expected: '1d' },
      { input: 129600000, expected: '1.5d' },
      { input: 691200000, expected: '8d' },
      // Months (approximated)
      { input: 2592000000, expected: '1mo' },
      { input: 3888000000, expected: '1.5mo' },
      { input: 8333333333, expected: '3.2mo' },
    ];

    it.each(testCases)('should format $input ms as "$expected"', ({ input, expected }) => {
      expect(pipe.transform(input)).toBe(expected);
    });
  });
});
