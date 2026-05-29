/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PortalAnalyticsFiltersService } from './portal-analytics-filters.service';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('PortalAnalyticsFiltersService', () => {
  let service: PortalAnalyticsFiltersService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PortalAnalyticsFiltersService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    });

    service = TestBed.inject(PortalAnalyticsFiltersService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_return_four_filter_definitions', done => {
    service.getDefinitions().subscribe(definitions => {
      expect(definitions).toHaveLength(4);
      expect(definitions.map(d => d.name)).toEqual(['API', 'APPLICATION', 'HTTP_STATUS_CODE_GROUP', 'HTTP_STATUS']);
      expect(definitions[0].type).toBe('KEYWORD');
      expect(definitions[1].type).toBe('KEYWORD');
      expect(definitions[2].type).toBe('ENUM');
      expect(definitions[2].values).toEqual(['1XX', '2XX', '3XX', '4XX', '5XX']);
      expect(definitions[3].type).toBe('NUMBER');
      expect(definitions[3].range).toEqual({ min: 100, max: 599 });
      expect(definitions[3].operators).toEqual(['EQ', 'LTE', 'GTE']);
      done();
    });
  });

  it('should_search_apis_for_api_filter_values', done => {
    service.getValues({ filterName: 'API', page: 1, perPage: 10, query: 'test' }).subscribe(result => {
      expect(result.data).toEqual([
        { value: 'api-1', label: 'My API' },
        { value: 'api-2', label: 'Other API' },
      ]);
      expect(result.hasNextPage).toBe(true);
      done();
    });

    httpTestingController
      .expectOne(req => req.url === `${TESTING_BASE_URL}/apis/_search`)
      .flush({
        data: [
          { id: 'api-1', name: 'My API' },
          { id: 'api-2', name: 'Other API' },
        ],
        metadata: { pagination: { current_page: 1, total_pages: 2 } },
      });
  });

  it('should_list_applications_for_application_filter_values', done => {
    service.getValues({ filterName: 'APPLICATION', page: 1, perPage: 10 }).subscribe(result => {
      expect(result.data).toEqual([{ value: 'app-1', label: 'My App' }]);
      expect(result.hasNextPage).toBe(false);
      done();
    });

    httpTestingController
      .expectOne(req => req.url.includes('/applications'))
      .flush({
        data: [{ id: 'app-1', name: 'My App' }],
        metadata: { pagination: { current_page: 1, total_pages: 1 } },
      });
  });

  it('should_return_empty_for_enum_status_code_group_filter', done => {
    // ENUM filters source their options from the definition's `values`, not getValues().
    service.getValues({ filterName: 'HTTP_STATUS_CODE_GROUP', page: 1, perPage: 10 }).subscribe(result => {
      expect(result.data).toEqual([]);
      expect(result.hasNextPage).toBe(false);
      done();
    });
  });

  it('should_return_empty_for_unknown_filter', done => {
    service.getValues({ filterName: 'UNKNOWN', page: 1, perPage: 10 }).subscribe(result => {
      expect(result.data).toEqual([]);
      expect(result.hasNextPage).toBe(false);
      done();
    });
  });
});
