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

import { CategoryConverterService } from './category-converter.service';
import { FacetsResponse } from '../../../widget/model/response/facets-response';

describe('CategoryConverterService', () => {
  let service: CategoryConverterService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CategoryConverterService],
    });
    service = TestBed.inject(CategoryConverterService);
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
            unit: 'NUMBER',
            buckets: [
              { key: 'tools/call', name: 'tools/call', measures: [{ name: 'COUNT', value: 17 }] },
              { key: 'tools/list', name: 'tools/list', measures: [{ name: 'COUNT', value: 14 }] },
              { key: 'resources/list', name: 'resources/list', measures: [{ name: 'COUNT', value: 10 }] },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result.labels).toEqual(['tools/call', 'tools/list', 'resources/list']);
      expect(result.datasets.length).toBe(1);
      expect(result.datasets[0].data).toEqual([17, 14, 10]);
    });

    it('should return empty arrays when metrics is empty', () => {
      const data: FacetsResponse = { metrics: [] };

      const result = service.convert(data);

      expect(result.labels).toEqual([]);
      expect(result.datasets).toEqual([{ data: [] }]);
    });

    it('should skip buckets without measures', () => {
      const data: FacetsResponse = {
        metrics: [
          {
            name: 'HTTP_REQUESTS',
            unit: 'NUMBER',
            buckets: [
              { key: 'tools/call', name: 'tools/call', measures: [{ name: 'COUNT', value: 17 }] },
              { key: 'empty', name: 'empty', measures: [] },
              { key: 'tools/list', name: 'tools/list', measures: [{ name: 'COUNT', value: 14 }] },
            ],
          },
        ],
      };

      const result = service.convert(data);

      expect(result.labels).toEqual(['tools/call', 'tools/list']);
      expect(result.datasets[0].data).toEqual([17, 14]);
    });
  });
});
