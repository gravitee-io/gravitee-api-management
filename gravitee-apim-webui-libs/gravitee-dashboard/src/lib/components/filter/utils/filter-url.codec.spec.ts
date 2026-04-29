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
import { FilterCondition } from '../filter.model';
import { decodeViewState, encodeViewState, ViewStateTimeframe } from './filter-url.codec';

describe('filter-url.codec', () => {
  const defaultTimeframe: ViewStateTimeframe = { period: '5m', from: null, to: null };

  describe('encodeViewState', () => {
    it('should return null when no filters and default timeframe', () => {
      expect(encodeViewState([], defaultTimeframe)).toBeNull();
    });

    it('should encode filters with string value for single-value condition', () => {
      const conditions: FilterCondition[] = [{ field: 'status', label: 'Status', operator: 'EQ', values: ['ACTIVE'] }];
      const result = encodeViewState(conditions, defaultTimeframe)!;
      const parsed = JSON.parse(result.q);
      expect(parsed.filter[0]).toEqual({ field: 'status', operator: 'EQ', value: 'ACTIVE' });
      expect(parsed.filter[0].label).toBeUndefined();
      expect(parsed.filter[0].valueLabels).toBeUndefined();
      expect(result.v).toBe('1');
    });

    it('should encode filters with array value for multi-value condition', () => {
      const conditions: FilterCondition[] = [{ field: 'tag', label: 'Tag', operator: 'IN', values: ['a', 'b', 'c'] }];
      const result = encodeViewState(conditions, defaultTimeframe)!;
      const parsed = JSON.parse(result.q);
      expect(parsed.filter[0]).toEqual({ field: 'tag', operator: 'IN', value: ['a', 'b', 'c'] });
    });

    it('should never include label or valueLabels in output', () => {
      const conditions: FilterCondition[] = [
        { field: 'API', label: 'API', operator: 'EQ', values: ['uuid-1'], valueLabels: ['My Cool API'] },
      ];
      const result = encodeViewState(conditions, defaultTimeframe)!;
      const parsed = JSON.parse(result.q);
      expect(parsed.filter[0].label).toBeUndefined();
      expect(parsed.filter[0].valueLabels).toBeUndefined();
    });

    it('should encode relative timerange when not default', () => {
      const tf: ViewStateTimeframe = { period: '1h', from: null, to: null };
      const result = encodeViewState([], tf)!;
      const parsed = JSON.parse(result.q);
      expect(parsed.time_range).toEqual({ type: 'relative', period: '1h' });
      expect(parsed.filter).toBeUndefined();
    });

    it('should ignore invalid relative timerange while encoding', () => {
      expect(encodeViewState([], { period: '5x', from: null, to: null })).toBeNull();
    });

    it('should encode absolute (custom) timerange', () => {
      const tf: ViewStateTimeframe = { period: 'custom', from: 1000000, to: 2000000 };
      const result = encodeViewState([], tf)!;
      const parsed = JSON.parse(result.q);
      expect(parsed.time_range).toEqual({ type: 'absolute', from: 1000000, to: 2000000 });
    });

    it('should encode both filters and timerange together', () => {
      const conditions: FilterCondition[] = [{ field: 'API', label: 'API', operator: 'EQ', values: ['id-1'] }];
      const tf: ViewStateTimeframe = { period: '1d', from: null, to: null };
      const result = encodeViewState(conditions, tf)!;
      const parsed = JSON.parse(result.q);
      expect(parsed.filter).toHaveLength(1);
      expect(parsed.time_range).toEqual({ type: 'relative', period: '1d' });
    });

    it('should omit time_range for default period (5m)', () => {
      const conditions: FilterCondition[] = [{ field: 'env', label: 'env', operator: 'EQ', values: ['prod'] }];
      const result = encodeViewState(conditions, defaultTimeframe)!;
      const parsed = JSON.parse(result.q);
      expect(parsed.time_range).toBeUndefined();
    });
  });

  describe('decodeViewState', () => {
    it('should return defaults when q is null', () => {
      const result = decodeViewState(null, '1');
      expect(result.conditions).toEqual([]);
      expect(result.timeframe).toEqual({ period: '5m', from: null, to: null });
    });

    it('should return defaults when version is wrong', () => {
      const result = decodeViewState('{"filter":[]}', '99');
      expect(result.conditions).toEqual([]);
    });

    it('should return defaults and warn for invalid JSON', () => {
      const warnSpy = jest.spyOn(console, 'warn').mockImplementation();
      const result = decodeViewState('not-json', '1');
      expect(result.conditions).toEqual([]);
      expect(warnSpy).toHaveBeenCalledWith('[filter-url-codec] Failed to parse q parameter, ignoring');
      warnSpy.mockRestore();
    });

    it('should decode filters with label fallback to field', () => {
      const q = JSON.stringify({ filter: [{ field: 'env', operator: 'EQ', value: 'prod' }] });
      const result = decodeViewState(q, '1');
      expect(result.conditions).toEqual([{ field: 'env', label: 'env', operator: 'EQ', values: ['prod'] }]);
    });

    it('should decode relative timerange', () => {
      const q = JSON.stringify({ time_range: { type: 'relative', period: '1h' } });
      const result = decodeViewState(q, '1');
      expect(result.timeframe).toEqual({ period: '1h', from: null, to: null });
    });

    it('should default timeframe when relative period is invalid', () => {
      const q = JSON.stringify({ time_range: { type: 'relative', period: '5x' } });
      const result = decodeViewState(q, '1');
      expect(result.timeframe).toEqual({ period: '5m', from: null, to: null });
    });

    it('should decode absolute timerange', () => {
      const q = JSON.stringify({ time_range: { type: 'absolute', from: 1000, to: 2000 } });
      const result = decodeViewState(q, '1');
      expect(result.timeframe).toEqual({ period: 'custom', from: 1000, to: 2000 });
    });

    it('should default timeframe when time_range is absent', () => {
      const q = JSON.stringify({ filter: [{ field: 'API', operator: 'EQ', value: 'x' }] });
      const result = decodeViewState(q, '1');
      expect(result.timeframe).toEqual({ period: '5m', from: null, to: null });
    });

    it('should filter out entries without field or operator', () => {
      const q = JSON.stringify({
        filter: [
          { field: 'env', operator: 'EQ', value: 'prod' },
          { field: '', operator: 'EQ', value: 'bad' },
          { field: 'tag', operator: '', value: 'bad' },
        ],
      });
      const result = decodeViewState(q, '1');
      expect(result.conditions).toHaveLength(1);
      expect(result.conditions[0].field).toBe('env');
    });

    it('should handle empty filter array with timerange', () => {
      const q = JSON.stringify({ filter: [], time_range: { type: 'relative', period: '1w' } });
      const result = decodeViewState(q, '1');
      expect(result.conditions).toEqual([]);
      expect(result.timeframe.period).toBe('1w');
    });

    it.each([{ filter: { field: 'API' } }, { filter: 'API' }, { filter: null }])(
      'should ignore malformed filter payload and preserve valid timerange',
      payload => {
        const q = JSON.stringify({ ...payload, time_range: { type: 'relative', period: '1h' } });
        const result = decodeViewState(q, '1');
        expect(result.conditions).toEqual([]);
        expect(result.timeframe).toEqual({ period: '1h', from: null, to: null });
      },
    );
  });

  describe('roundtrip', () => {
    it('should roundtrip filters and relative timerange', () => {
      const conditions: FilterCondition[] = [
        { field: 'API', label: 'API', operator: 'EQ', values: ['uuid-1'], valueLabels: ['My API'] },
        { field: 'tag', label: 'Tag', operator: 'IN', values: ['x', 'y'] },
      ];
      const tf: ViewStateTimeframe = { period: '1h', from: null, to: null };

      const encoded = encodeViewState(conditions, tf)!;
      const decoded = decodeViewState(encoded.q, encoded.v);

      expect(decoded.conditions).toHaveLength(2);
      expect(decoded.conditions[0].field).toBe('API');
      expect(decoded.conditions[0].label).toBe('API');
      expect(decoded.conditions[0].values).toEqual(['uuid-1']);
      expect(decoded.conditions[0].valueLabels).toBeUndefined();
      expect(decoded.conditions[1].values).toEqual(['x', 'y']);
      expect(decoded.timeframe).toEqual({ period: '1h', from: null, to: null });
    });

    it('should roundtrip custom timerange', () => {
      const tf: ViewStateTimeframe = { period: 'custom', from: 5000, to: 9000 };
      const encoded = encodeViewState([], tf)!;
      const decoded = decodeViewState(encoded.q, encoded.v);
      expect(decoded.timeframe).toEqual({ period: 'custom', from: 5000, to: 9000 });
    });

    it('should roundtrip default timeframe producing null', () => {
      const encoded = encodeViewState([], { period: '5m', from: null, to: null });
      expect(encoded).toBeNull();
    });
  });
});
