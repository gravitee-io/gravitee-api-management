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

  // --- Test Cases for Invalid and Edge Case Inputs ---

  it('should return "N/A" for null input', () => {
    expect(pipe.transform(null)).toBe('N/A');
  });

  it('should return "N/A" for undefined input', () => {
    expect(pipe.transform(undefined)).toBe('N/A');
  });

  it('should return "N/A" for NaN input', () => {
    expect(pipe.transform(NaN)).toBe('N/A');
  });

  it('should return "0" for an input of 0', () => {
    expect(pipe.transform(0)).toBe('0');
  });

  // --- Test Cases for Numbers Less Than 1000 ---

  it('should return the number as a string if it is less than 1000', () => {
    expect(pipe.transform(123)).toBe('123');
    expect(pipe.transform(999)).toBe('999');
  });

  it('should handle negative numbers less than 1000', () => {
    expect(pipe.transform(-123)).toBe('-123');
    expect(pipe.transform(-999)).toBe('-999');
  });

  // --- Test Cases for Thousands (K) ---

  it('should format 1000 as "1K"', () => {
    expect(pipe.transform(1000)).toBe('1K');
  });

  it('should format numbers in the thousands with one decimal place', () => {
    expect(pipe.transform(1234)).toBe('1.2K');
  });

  it('should round numbers in the thousands correctly', () => {
    expect(pipe.transform(9876)).toBe('9.9K'); // Rounds up
    expect(pipe.transform(1990)).toBe('2K'); // Rounds up and removes .0
  });

  // --- Test Cases for Millions (M) ---

  it('should format 1,000,000 as "1M"', () => {
    expect(pipe.transform(1_000_000)).toBe('1M');
  });

  it('should format numbers in the millions with one decimal place', () => {
    expect(pipe.transform(1_550_000)).toBe('1.6M');
  });

  it('should format large millions correctly', () => {
    expect(pipe.transform(123_456_789)).toBe('123.5M');
  });

  // --- Test Cases for Billions (B) ---

  it('should format 1,000,000,000 as "1B"', () => {
    expect(pipe.transform(1_000_000_000)).toBe('1B');
  });

  it('should format numbers in the billions with one decimal place', () => {
    expect(pipe.transform(2_345_000_000)).toBe('2.3B');
  });
});
