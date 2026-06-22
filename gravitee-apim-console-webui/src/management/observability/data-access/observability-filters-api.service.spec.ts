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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { ObservabilityFiltersApiService } from './observability-filters-api.service';

import { Constants } from '../../../entities/Constants';
import { CONSTANTS_TESTING } from '../../../shared/testing/gio-testing.module';

describe('ObservabilityFiltersApiService', () => {
  let service: ObservabilityFiltersApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ObservabilityFiltersApiService,
        { provide: Constants, useValue: CONSTANTS_TESTING },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ObservabilityFiltersApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should GET definitions and map enumValues to values', done => {
    const base = CONSTANTS_TESTING.env!.v2BaseURL!;
    service.getDefinitions().subscribe(defs => {
      expect(defs[0]).toEqual(
        expect.objectContaining({
          name: 'HTTP_METHOD',
          label: 'Method',
          type: 'ENUM',
          values: ['GET', 'POST'],
        }),
      );
      done();
    });
    const req = httpMock.expectOne(`${base}/observability/filters/definition`);
    expect(req.request.method).toBe('GET');
    req.flush({
      data: [
        {
          name: 'HTTP_METHOD',
          label: 'Method',
          type: 'ENUM',
          operators: ['EQ'],
          enumValues: ['GET', 'POST'],
        },
      ],
    });
  });

  it('should GET values with query params and derive hasNextPage from pagination', done => {
    const base = CONSTANTS_TESTING.env!.v2BaseURL!;
    service
      .getValues({
        filterName: 'API',
        query: 'foo',
        from: 1,
        to: 2,
        page: 1,
        perPage: 10,
      })
      .subscribe(result => {
        expect(result.data).toEqual([{ value: 'uuid-1', label: 'My API', id: 'uuid-1' }]);
        expect(result.hasNextPage).toBe(true);
        expect(result.totalCount).toBe(25);
        done();
      });
    const req = httpMock.expectOne(r => r.url === `${base}/observability/filters/API/values`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('query')).toBe('foo');
    expect(req.request.params.get('from')).toBe('1');
    expect(req.request.params.get('to')).toBe('2');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('perPage')).toBe('10');
    req.flush({
      data: [{ value: 'My API', id: 'uuid-1' }],
      pagination: { page: 1, perPage: 10, pageItemsCount: 1, totalCount: 25 },
    });
  });

  it('should derive hasNextPage from pageCount when present', done => {
    const base = CONSTANTS_TESTING.env!.v2BaseURL!;
    service.getValues({ filterName: 'API', page: 2, perPage: 10 }).subscribe(result => {
      expect(result.hasNextPage).toBe(false);
      done();
    });
    httpMock
      .expectOne(r => r.url.startsWith(`${base}/observability/filters/API/values`))
      .flush({
        data: [{ value: 'x' }],
        pagination: { page: 2, pageCount: 2, perPage: 10, totalCount: 15 },
      });
  });

  it('should map signals from API response to FilterDefinition', done => {
    const base = CONSTANTS_TESTING.env!.v2BaseURL!;
    service.getDefinitions().subscribe(defs => {
      expect(defs[0]).toEqual(
        expect.objectContaining({
          name: 'PAYLOAD',
          label: 'Payload content',
          type: 'STRING',
          operators: ['CONTAINS'],
          signals: ['LOGS'],
          apiTypes: ['HTTP_PROXY', 'LLM', 'MCP'],
        }),
      );
      done();
    });
    const req = httpMock.expectOne(`${base}/observability/filters/definition`);
    req.flush({
      data: [
        {
          name: 'PAYLOAD',
          label: 'Payload content',
          type: 'STRING',
          operators: ['CONTAINS'],
          apiTypes: ['HTTP_PROXY', 'LLM', 'MCP'],
          signals: ['LOGS'],
        },
      ],
    });
  });

  it('should map definition without signals to undefined', done => {
    const base = CONSTANTS_TESTING.env!.v2BaseURL!;
    service.getDefinitions().subscribe(defs => {
      expect(defs[0].signals).toBeUndefined();
      done();
    });
    const req = httpMock.expectOne(`${base}/observability/filters/definition`);
    req.flush({
      data: [{ name: 'URI', label: 'HTTP Path', type: 'STRING', operators: ['EQ'] }],
    });
  });

  it('should set hasNextPage from page size when pagination is absent', done => {
    const base = CONSTANTS_TESTING.env!.v2BaseURL!;
    service.getValues({ filterName: 'GATEWAY', page: 1, perPage: 10 }).subscribe(result => {
      expect(result.hasNextPage).toBe(true);
      done();
    });
    httpMock
      .expectOne(r => r.url.startsWith(`${base}/observability/filters/GATEWAY/values`))
      .flush({
        data: new Array(10).fill(0).map((_, i) => ({ value: `g${i}` })),
      });
  });
});
