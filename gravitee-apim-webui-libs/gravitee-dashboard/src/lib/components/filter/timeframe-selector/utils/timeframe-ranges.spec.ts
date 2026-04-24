/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { epochMsRangeFromDashboardQueryParams, timeInMilliseconds } from './timeframe-ranges';

describe('epochMsRangeFromDashboardQueryParams', () => {
  const fixedNow = new Date('2025-06-01T12:00:00.000Z').getTime();

  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(fixedNow);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should default to 5m when period is missing', () => {
    const { from, to } = epochMsRangeFromDashboardQueryParams({});
    expect(to).toBe(fixedNow);
    expect(from).toBe(fixedNow - timeInMilliseconds['5m']);
  });

  it('should resolve 1d preset', () => {
    const { from, to } = epochMsRangeFromDashboardQueryParams({ period: '1d' });
    expect(to).toBe(fixedNow);
    expect(from).toBe(fixedNow - timeInMilliseconds['1d']);
  });

  it('should fall back to 5m for unknown period', () => {
    const { from, to } = epochMsRangeFromDashboardQueryParams({ period: 'unknown' });
    expect(to).toBe(fixedNow);
    expect(from).toBe(fixedNow - timeInMilliseconds['5m']);
  });

  it('should use first value when period is an array', () => {
    const { from, to } = epochMsRangeFromDashboardQueryParams({ period: ['1h', '1d'] });
    expect(to).toBe(fixedNow);
    expect(from).toBe(fixedNow - timeInMilliseconds['1h']);
  });

  it('should parse custom range from epoch strings', () => {
    const fromMs = 1_700_000_000_000;
    const toMs = 1_700_000_360_000;
    const { from, to } = epochMsRangeFromDashboardQueryParams({
      period: 'custom',
      from: String(fromMs),
      to: String(toMs),
    });
    expect(from).toBe(fromMs);
    expect(to).toBe(toMs);
  });
});
