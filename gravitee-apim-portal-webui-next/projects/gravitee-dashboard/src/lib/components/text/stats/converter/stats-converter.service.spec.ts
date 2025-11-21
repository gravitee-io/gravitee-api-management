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

import { StatsConverterService } from './stats-converter.service';
import { MeasuresResponse } from '../../../widget/model/response/measures-response';

describe('StatsConverterService', () => {
  let service: StatsConverterService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(StatsConverterService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('convert', () => {
    it('should convert measures with different unit types', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [
              { name: 'AVG', value: 45.6 },
              { name: 'MIN', value: 10 },
              { name: 'MAX', value: 1200 },
              { name: 'COUNT', value: 1234 },
              { name: 'RPS', value: 5.5 },
              { name: 'PERCENTAGE', value: 75.8 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['46 ms', '10 ms', '1,200 ms', '1,234', '6 req/s', '76 %']);
    });

    it('should truncate decimal values using Math.trunc', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [
              { name: 'AVG', value: 45.999 },
              { name: 'MIN', value: 10.123 },
              { name: 'MAX', value: 1200.789 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['46 ms', '10 ms', '1,201 ms']);
    });

    it('should handle zero values', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [
              { name: 'AVG', value: 0 },
              { name: 'COUNT', value: 0 },
              { name: 'RPS', value: 0 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['0 ms', '0', '0 req/s']);
    });

    it('should handle negative values', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [
              { name: 'AVG', value: -45.6 },
              { name: 'MIN', value: -10 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['-46 ms', '-10 ms']);
    });

    it('should handle large values with proper formatting', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [
              { name: 'COUNT', value: 1234567 },
              { name: 'AVG', value: 999999.99 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['1,234,567', '1,000,000 ms']);
    });

    it('should handle percentile measures (P50, P90, P95, P99)', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [
              { name: 'P50', value: 50.4 },
              { name: 'P90', value: 90.2 },
              { name: 'P95', value: 95.94 },
              { name: 'P99', value: 99.99 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['50 ms', '90 ms', '96 ms', '100 ms']);
    });

    it('should return empty array when metrics is undefined', () => {
      const data: MeasuresResponse = {
        metrics: undefined,
      };

      const result = service.convert(data);

      expect(result).toEqual([]);
    });

    it('should return empty array when metrics is empty', () => {
      const data: MeasuresResponse = {
        metrics: [],
      };

      const result = service.convert(data);

      expect(result).toEqual([]);
    });

    it('should return empty array when measures is empty', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual([]);
    });

    it('should only use the first metric when multiple metrics are present', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [
              { name: 'AVG', value: 45 },
              { name: 'COUNT', value: 100 },
            ],
          },
          {
            name: 'HTTP_ERRORS',
            measures: [
              { name: 'AVG', value: 999 },
              { name: 'COUNT', value: 200 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['45 ms', '100']);
    });

    it('should handle single measure', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [{ name: 'AVG', value: 42 }],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['42 ms']);
    });

    it('should format numbers according to locale', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            measures: [{ name: 'COUNT', value: 1234567 }],
          },
        ],
      };

      const result = service.convert(data);

      // toLocaleString() should format with thousand separators
      expect(result[0]).toContain(',');
      expect(result[0]).toBe('1,234,567');
    });
  });
});
