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
import { TestBed } from '@angular/core/testing';

import { BarConverterService } from './bar-converter.service';
import { TimeSeriesBucket, TimeSeriesResponse } from '../../../widget/model/response/time-series-response';

describe('BarConverterService', () => {
  let service: BarConverterService;
  const makeBaseBucket = (key: string, overrides: Partial<TimeSeriesBucket> = {}): TimeSeriesBucket => ({
    key,
    name: key,
    timestamp: new Date(key),
    ...overrides,
  });

  const makeMeasureBucket = (key: string, value: number): TimeSeriesBucket =>
    makeBaseBucket(key, {
      measures: [{ name: 'COUNT', value }],
    });

  const makeNestedBucket = (key: string, groups: Record<string, number>): TimeSeriesBucket =>
    makeBaseBucket(key, {
      buckets: Object.entries(groups).map(([groupKey, value]) => ({
        key: groupKey,
        name: groupKey,
        measures: [{ name: 'COUNT', value }],
      })),
    });

  const makeResponse = (metrics: TimeSeriesResponse['metrics'] = []): TimeSeriesResponse => {
    const firstMetricBuckets = metrics[0]?.buckets ?? [];
    const buckets = (firstMetricBuckets as TimeSeriesBucket[]).map(b => ({
      key: b.key,
      name: b.name,
      timestamp: b.timestamp,
    }));

    return {
      metrics,
      buckets,
    };
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [BarConverterService],
    });
    service = TestBed.inject(BarConverterService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('convert', () => {
    it('should convert TimeSeriesResponse to ChartData with labels and data', () => {
      const data: TimeSeriesResponse = makeResponse([
        {
          name: 'HTTP_REQUESTS',
          buckets: [makeMeasureBucket('2025-10-07T06:00:00Z', 100), makeMeasureBucket('2025-10-08T06:00:00Z', 200)],
        },
      ]);

      const result = service.convert(data);

      expect(result.labels?.length).toBe(2);
      expect(result.datasets.length).toBe(1);
      expect(result.datasets[0].data).toEqual([100, 200]);
      expect(result.datasets[0].label).toBe('HTTP_REQUESTS');
    });

    it('should return empty arrays when metrics is empty', () => {
      const data: TimeSeriesResponse = {
        metrics: [],
        buckets: [],
      };

      const result = service.convert(data);

      expect(result.labels).toEqual([]);
      expect(result.datasets).toEqual([]);
    });

    it('should handle nested buckets structure', () => {
      const data: TimeSeriesResponse = makeResponse([
        {
          name: 'HTTP_REQUESTS',
          buckets: [
            makeNestedBucket('2025-10-07T06:00:00Z', {
              '100-199': 50,
              '200-299': 30,
            }),
            makeNestedBucket('2025-10-08T06:00:00Z', {
              '100-199': 60,
              '200-299': 40,
            }),
          ],
        },
      ]);

      const result = service.convert(data);

      expect(result.labels?.length).toBe(2);
      expect(result.datasets.length).toBe(2);
      expect(result.datasets[0].label).toBe('100-199');
      expect(result.datasets[0].data).toEqual([50, 60]);
      expect(result.datasets[1].label).toBe('200-299');
      expect(result.datasets[1].data).toEqual([30, 40]);
    });

    it('should handle buckets without measures', () => {
      const data: TimeSeriesResponse = makeResponse([
        {
          name: 'HTTP_REQUESTS',
          buckets: [
            makeMeasureBucket('2025-10-07T06:00:00Z', 100),
            makeBaseBucket('2025-10-08T06:00:00Z', { measures: [] }),
            makeMeasureBucket('2025-10-09T06:00:00Z', 300),
          ],
        },
      ]);

      const result = service.convert(data);

      expect(result.labels?.length).toBe(3);
      expect(result.datasets[0].data).toEqual([100, 0, 300]);
    });

    it('should use timestamp when available, otherwise fallback to key', () => {
      const data: TimeSeriesResponse = makeResponse([
        {
          name: 'HTTP_REQUESTS',
          buckets: [
            makeBaseBucket('2025-10-07T06:00:00Z', {
              timestamp: new Date(1728288000000),
              measures: [{ name: 'COUNT', value: 100 }],
            }),
            makeBaseBucket('2025-10-08T06:00:00Z', {
              timestamp: undefined,
              measures: [{ name: 'COUNT', value: 200 }],
            }),
          ],
        },
      ]);

      const result = service.convert(data);

      expect(result.labels?.length).toBe(2);
      expect(result.labels?.[0]).toMatch(/^\d{4}-\d{2}-\d{2}T/);
      expect(result.labels?.[1]).toBe('2025-10-08T06:00:00Z');
    });

    it('should handle multiple metrics', () => {
      const data: TimeSeriesResponse = makeResponse([
        {
          name: 'HTTP_REQUESTS',
          buckets: [makeMeasureBucket('2025-10-07T06:00:00Z', 100)],
        },
        {
          name: 'HTTP_ERRORS',
          buckets: [makeMeasureBucket('2025-10-07T06:00:00Z', 5)],
        },
      ]);

      const result = service.convert(data);

      expect(result.labels?.length).toBe(1);
      expect(result.datasets.length).toBe(2);
      expect(result.datasets[0].label).toBe('HTTP_REQUESTS');
      expect(result.datasets[0].data).toEqual([100]);
      expect(result.datasets[1].label).toBe('HTTP_ERRORS');
      expect(result.datasets[1].data).toEqual([5]);
    });

    it('should create separate datasets for nested buckets without direct measures', () => {
      const data: TimeSeriesResponse = makeResponse([
        {
          name: 'HTTP_REQUESTS',
          buckets: [
            makeBaseBucket('2025-10-07T06:00:00Z', {
              buckets: [
                {
                  key: '100-199',
                  name: '100-199',
                  measures: [{ name: 'COUNT', value: 50 }],
                },
                {
                  key: '200-299',
                  name: '200-299',
                  measures: [{ name: 'COUNT', value: 30 }],
                },
              ],
            }),
          ],
        },
      ]);

      const result = service.convert(data);

      expect(result.labels?.length).toBe(1);
      expect(result.datasets.length).toBe(2);
      expect(result.datasets[0].label).toBe('100-199');
      expect(result.datasets[1].label).toBe('200-299');
    });
    it('should prioritize data.buckets over metric buckets for labels', () => {
      // Use keys and undefined timestamps to test logic without date parsing interference
      const metricBuckets = [makeMeasureBucket('metric-key-1', 100)];
      metricBuckets[0].timestamp = undefined;

      const globalBuckets = [makeBaseBucket('global-key-1'), makeBaseBucket('global-key-2')];
      globalBuckets.forEach(b => (b.timestamp = undefined));

      const data: TimeSeriesResponse = {
        metrics: [{ name: 'HTTP_REQUESTS', buckets: metricBuckets }],
        buckets: globalBuckets,
      };

      const result = service.convert(data);

      expect(result.labels).toEqual(['global-key-1', 'global-key-2']);
      expect(result.datasets[0].data).toEqual([100]);
    });
  });
});
