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

import { PieConverterService } from './pie-converter.service';
import { FacetsResponse } from '../../../widget/model/response/facets-response';

describe('PieConverterService', () => {
  let service: PieConverterService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PieConverterService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('convert', () => {
    it('should convert FacetsResponse to ChartData with labels and data', () => {
      const data: FacetsResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            buckets: [
              {
                key: 'bucket-1',
                name: 'bucket-1',
                measures: [{ name: 'COUNT', value: 100 }],
              },
              {
                key: 'bucket-2',
                name: 'bucket-2',
                measures: [{ name: 'COUNT', value: 200 }],
              },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result.labels).toEqual(['bucket-1', 'bucket-2']);
      expect(result.datasets[0].data).toEqual([100, 200]);
    });

    it('should return empty arrays when metrics is empty', () => {
      const data: FacetsResponse = {
        metrics: [],
      };

      const result = service.convert(data);

      expect(result.labels).toEqual([]);
      expect(result.datasets[0].data).toEqual([]);
    });

    it('should filter out buckets without measures', () => {
      const data: FacetsResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            buckets: [
              {
                key: 'bucket-1',
                name: 'bucket-1',
                measures: [{ name: 'COUNT', value: 100 }],
              },
              {
                key: 'bucket-2',
                name: 'bucket-2',
                measures: [],
              },
              {
                key: 'bucket-3',
                name: 'bucket-3',
                measures: [{ name: 'COUNT', value: 300 }],
              },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result.labels).toEqual(['bucket-1', 'bucket-3']);
      expect(result.datasets[0].data).toEqual([100, 300]);
    });
  });
});
