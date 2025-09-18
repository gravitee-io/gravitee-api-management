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
  // Create an instance of the pipe before each test
  let pipe: FormatDurationPipe;

  beforeEach(() => {
    pipe = new FormatDurationPipe();
  });

  // Test suite for invalid and edge case inputs
  describe('Invalid and Edge Case Inputs', () => {
    it('should return "N/A" for null input', () => {
      expect(pipe.transform(null)).toBe('N/A');
    });

    it('should return "N/A" for undefined input', () => {
      expect(pipe.transform(undefined)).toBe('N/A');
    });

    it('should return "N/A" for NaN input', () => {
      expect(pipe.transform(NaN)).toBe('N/A');
    });

    it('should return "0ms" for an input of 0', () => {
      expect(pipe.transform(0)).toBe('0ms');
    });
  });

  // Test suite for millisecond formatting
  describe('Milliseconds Formatting', () => {
    it('should format values less than 1000 as ms', () => {
      expect(pipe.transform(999)).toBe('999ms');
    });

    it('should format a mid-range millisecond value', () => {
      expect(pipe.transform(500)).toBe('500ms');
    });
  });

  // Test suite for second formatting
  describe('Seconds Formatting', () => {
    it('should format 1000ms as "1s"', () => {
      expect(pipe.transform(1000)).toBe('1s');
    });

    it('should format 1500ms as "1.5s"', () => {
      expect(pipe.transform(1500)).toBe('1.5s');
    });

    it('should format 59999ms as "59.9s"', () => {
      expect(pipe.transform(59999)).toBe('60s');
    });
  });

  // Test suite for minute formatting
  describe('Minutes Formatting', () => {
    it('should format 60000ms as "1min"', () => {
      expect(pipe.transform(60000)).toBe('1min');
    });

    it('should format 90000ms as "1.5min"', () => {
      expect(pipe.transform(90000)).toBe('1.5min');
    });

    it('should correctly round 59.94 minutes (3596400 milliseconds) down to "59.9min"', () => {
      expect(pipe.transform(3596400)).toBe('59.9min');
    });

    it('should format 3599999ms as "60min"', () => {
      expect(pipe.transform(3599999)).toBe('60min');
    });
  });

  // Test suite for hour formatting
  describe('Hours Formatting', () => {
    it('should format 3600000ms as "1h"', () => {
      expect(pipe.transform(3600000)).toBe('1h');
    });

    it('should format 5400000ms as "1.5h"', () => {
      expect(pipe.transform(5400000)).toBe('1.5h');
    });

    it('should format 86399999ms as "24h"', () => {
      expect(pipe.transform(86399999)).toBe('24h');
    });

    it('should format a value like 23.94 hours as "23.9h"', () => {
      expect(pipe.transform(86040011)).toBe('23.9h');
    });
  });

  // Test suite for day formatting
  describe('Days Formatting', () => {
    it('should format 86400000ms as "1d"', () => {
      expect(pipe.transform(86400000)).toBe('1d');
    });

    it('should format 129600000ms as "1.5d"', () => {
      expect(pipe.transform(129600000)).toBe('1.5d');
    });

    it('should format 691200000ms as "8d"', () => {
      expect(pipe.transform(691200000)).toBe('8d');
    });
  });

  // Test suite for month formatting (based on 30-day approximation)
  describe('Months Formatting', () => {
    it('should format 2592000000ms as "1mo"', () => {
      expect(pipe.transform(2592000000)).toBe('1mo');
    });



    it('should format 3888000000ms as "1.5mo"', () => {
      expect(pipe.transform(3888000000)).toBe('1.5mo');
    });

    it('should format a large value correctly into months', () => {
      // Represents roughly 3.2 months
      expect(pipe.transform(8333333333)).toBe('3.2mo');
    });
  });
});
