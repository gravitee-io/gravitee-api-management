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

import { duration } from 'moment';

import { WindowedCount } from './windowed-count';

describe('WindowedCount', () => {
  const durationSec = (seconds: number) => {
    return duration(seconds, 'seconds');
  };

  describe('constructor', () => {
    it('should correctly set count and window', () => {
      const window = durationSec(10);
      const windowedCount = new WindowedCount(5, window);
      expect(windowedCount.count).toBe(5);
      expect(windowedCount.window).toBe(window);
    });
  });

  describe('rate', () => {
    it('should calculate the correct rate of events per second', () => {
      const window = durationSec(10);
      const windowedCount = new WindowedCount(20, window);
      expect(windowedCount.rate()).toBe(2); // 20 events / 10 seconds = 2
    });

    it('should return 0 when count is 0', () => {
      const window = durationSec(10);
      const windowedCount = new WindowedCount(0, window);
      expect(windowedCount.rate()).toBe(0);
    });

    it('should correctly handle an extremely small duration', () => {
      const window = durationSec(0.1);
      const windowedCount = new WindowedCount(5, window);
      expect(windowedCount.rate()).toBeCloseTo(50); // 5 / 0.1 = 50
    });
    it('should correctly handle an extremely large counbt', () => {
      const window = durationSec(50);
      const windowedCount = new WindowedCount(1, window);
      expect(windowedCount.rate()).toBeCloseTo(0.02); // 1/50 = 0.02
    });
  });

  describe('parse', () => {
    it('should correctly parse a valid format string', () => {
      const windowedCount = WindowedCount.parse('10/PT1M');
      expect(windowedCount.count).toBe(10);
      expect(windowedCount.window.asSeconds()).toBe(60);
      const reparsed = WindowedCount.parse(windowedCount.encode());
      expect(reparsed.count).toBe(10);
      expect(reparsed.window.asSeconds()).toBe(60);
    });

    it('should throw an error for an invalid format', () => {
      expect(() => WindowedCount.parse('10')).toThrow('Invalid format');
      expect(() => WindowedCount.parse('10-20')).toThrow('Invalid format');
    });

    it('should throw an error if count is not a number', () => {
      expect(() => WindowedCount.parse('foo/PT1S')).toThrow('Invalid format');
    });
    it('should throw an error if window is not a duration', () => {
      expect(() => WindowedCount.parse('1/bar')).toThrow('Invalid format');
    });
  });

  describe('isValid', () => {
    it('should return true if count is positive and duration is greater than zero', () => {
      const window = durationSec(60);
      const windowedCount = new WindowedCount(5, window);
      expect(windowedCount.isValid()).toBe(true);
    });

    it('should return false if count is zero or negative', () => {
      const window = durationSec(60);
      const windowedCount = new WindowedCount(0, window);
      expect(windowedCount.isValid()).toBe(false);
    });

    it('should return false if duration is zero or negative', () => {
      const window = durationSec(0);
      const windowedCount = new WindowedCount(5, window);
      expect(windowedCount.isValid()).toBe(false);
    });
  });
});
