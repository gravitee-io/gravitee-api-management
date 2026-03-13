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
    it('should apply NUMBER unit to all measures', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            unit: 'NUMBER',
            measures: [
              { name: 'AVG', value: 45.6 },
              { name: 'MIN', value: 10 },
              { name: 'MAX', value: 1200 },
              { name: 'COUNT', value: 1234 },
              { name: 'VALUE', value: 75.8 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['46', '10', '1,200', '1,234', '76']);
    });

    it('should apply MILLISECONDS unit to all measures', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_GATEWAY_RESPONSE_TIME',
            unit: 'MILLISECONDS',
            measures: [
              { name: 'AVG', value: 45.6 },
              { name: 'MIN', value: 10 },
              { name: 'MAX', value: 1200 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['46 ms', '10 ms', '1,200 ms']);
    });

    it('should apply PERCENT unit to all measures', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_ERROR_RATE',
            unit: 'PERCENT',
            measures: [{ name: 'PERCENTAGE', value: 75.8 }],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['76 %']);
    });

    it('should apply BYTES unit to all measures', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            unit: 'BYTES',
            measures: [
              { name: 'AVG', value: 2048 },
              { name: 'MAX', value: 10240 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['2,048 bytes', '10,240 bytes']);
    });

    it('should round decimal values', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_GATEWAY_RESPONSE_TIME',
            unit: 'MILLISECONDS',
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
            unit: 'NUMBER',
            measures: [
              { name: 'AVG', value: 0 },
              { name: 'COUNT', value: 0 },
              { name: 'VALUE', value: 0 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['0', '0', '0']);
    });

    it('should handle negative values', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_GATEWAY_RESPONSE_TIME',
            unit: 'MILLISECONDS',
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
            unit: 'NUMBER',
            measures: [
              { name: 'COUNT', value: 1234567 },
              { name: 'AVG', value: 999999.99 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['1,234,567', '1,000,000']);
    });

    it('should handle percentile measures with MILLISECONDS unit', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_GATEWAY_RESPONSE_TIME',
            unit: 'MILLISECONDS',
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
            unit: 'NUMBER',
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
            unit: 'NUMBER',
            measures: [
              { name: 'AVG', value: 45 },
              { name: 'COUNT', value: 100 },
            ],
          },
          {
            name: 'HTTP_ERRORS',
            unit: 'NUMBER',
            measures: [
              { name: 'AVG', value: 999 },
              { name: 'COUNT', value: 200 },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['45', '100']);
    });

    it('should handle single measure', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            unit: 'NUMBER',
            measures: [{ name: 'AVG', value: 42 }],
          },
        ],
      };

      const result = service.convert(data);

      expect(result).toEqual(['42']);
    });

    it('should format numbers according to locale', () => {
      const data: MeasuresResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            unit: 'NUMBER',
            measures: [{ name: 'COUNT', value: 1234567 }],
          },
        ],
      };

      const result = service.convert(data);

      expect(result[0]).toContain(',');
      expect(result[0]).toBe('1,234,567');
    });
  });
});
