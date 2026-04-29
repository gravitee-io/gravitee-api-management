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
import { FilterCondition, FilterDefinition, FILTER_DEFINITION_PROVIDER, FilterDefinitionProvider } from '@gravitee/gravitee-dashboard';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { FilterLabelResolver } from './filter-label.resolver';

import { Constants } from '../../../../../entities/Constants';

describe('FilterLabelResolver', () => {
  let resolver: FilterLabelResolver;
  let httpMock: HttpTestingController;
  let definitionProvider: FilterDefinitionProvider;

  const DEFINITIONS: FilterDefinition[] = [
    { name: 'API', label: 'API', type: 'KEYWORD', operators: ['EQ', 'IN'] },
    { name: 'APPLICATION', label: 'Application', type: 'KEYWORD', operators: ['EQ', 'IN'] },
    { name: 'HTTP_STATUS', label: 'HTTP Status', type: 'KEYWORD', operators: ['EQ'] },
  ];

  beforeEach(() => {
    definitionProvider = { getDefinitions: jest.fn().mockReturnValue(of(DEFINITIONS)) };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        FilterLabelResolver,
        { provide: FILTER_DEFINITION_PROVIDER, useValue: definitionProvider },
        { provide: Constants, useValue: { env: { v2BaseURL: '/v2' } } },
      ],
    });

    resolver = TestBed.inject(FilterLabelResolver);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should return empty array for empty conditions', done => {
    resolver.resolveLabels([]).subscribe(result => {
      expect(result).toEqual([]);
      done();
    });
  });

  it('should resolve field labels from definitions and value labels from backend for ID-based filters', done => {
    const conditions: FilterCondition[] = [
      { field: 'API', label: 'API', operator: 'EQ', values: ['uuid-1'] },
      { field: 'APPLICATION', label: 'APPLICATION', operator: 'IN', values: ['app-1', 'app-2'] },
    ];

    resolver.resolveLabels(conditions).subscribe(result => {
      expect(result).toHaveLength(2);
      expect(result[0].label).toBe('API');
      expect(result[0].valueLabels).toEqual(['My Cool API']);
      expect(result[1].label).toBe('Application');
      expect(result[1].valueLabels).toEqual(['App One', 'app-2']);
      done();
    });

    const req = httpMock.expectOne('/v2/observability/filters/resolve');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.entries).toEqual([
      { filterName: 'API', ids: ['uuid-1'] },
      { filterName: 'APPLICATION', ids: ['app-1', 'app-2'] },
    ]);
    req.flush({
      entries: [
        { filterName: 'API', labels: { 'uuid-1': 'My Cool API' } },
        { filterName: 'APPLICATION', labels: { 'app-1': 'App One' } },
      ],
    });
  });

  it('should pass through non-ID-based conditions without calling resolve', done => {
    const conditions: FilterCondition[] = [{ field: 'HTTP_STATUS', label: 'HTTP_STATUS', operator: 'EQ', values: ['200'] }];

    resolver.resolveLabels(conditions).subscribe(result => {
      expect(result).toHaveLength(1);
      expect(result[0].label).toBe('HTTP Status');
      expect(result[0].valueLabels).toBeUndefined();
      done();
    });
  });

  it('should send only ID-based conditions when resolving a mixed filter set', done => {
    const conditions: FilterCondition[] = [
      { field: 'API', label: 'API', operator: 'EQ', values: ['uuid-1'] },
      { field: 'HTTP_STATUS', label: 'HTTP_STATUS', operator: 'EQ', values: ['200'] },
    ];

    resolver.resolveLabels(conditions).subscribe(result => {
      expect(result[0].valueLabels).toEqual(['My Cool API']);
      expect(result[1].label).toBe('HTTP Status');
      expect(result[1].valueLabels).toBeUndefined();
      done();
    });

    const req = httpMock.expectOne('/v2/observability/filters/resolve');
    expect(req.request.body.entries).toEqual([{ filterName: 'API', ids: ['uuid-1'] }]);
    req.flush({
      entries: [{ filterName: 'API', labels: { 'uuid-1': 'My Cool API' } }],
    });
  });

  it('should cache filter definitions across resolutions', done => {
    const firstConditions: FilterCondition[] = [{ field: 'API', label: 'API', operator: 'EQ', values: ['uuid-1'] }];
    const secondConditions: FilterCondition[] = [{ field: 'APPLICATION', label: 'APPLICATION', operator: 'EQ', values: ['app-1'] }];

    // The two resolutions must run sequentially so the second one observes the cached
    // definitions from the first. The inner subscribe is intentional here: a switchMap
    // pipeline would force us to interleave httpMock.expectOne flushes through a Subject.
    resolver.resolveLabels(firstConditions).subscribe(firstResult => {
      expect(firstResult[0].valueLabels).toEqual(['My Cool API']);

      // eslint-disable-next-line rxjs/no-nested-subscribe
      resolver.resolveLabels(secondConditions).subscribe(secondResult => {
        expect(secondResult[0].label).toBe('Application');
        expect(secondResult[0].valueLabels).toEqual(['App One']);
        expect(definitionProvider.getDefinitions).toHaveBeenCalledTimes(1);
        done();
      });

      const secondReq = httpMock.expectOne('/v2/observability/filters/resolve');
      secondReq.flush({
        entries: [{ filterName: 'APPLICATION', labels: { 'app-1': 'App One' } }],
      });
    });

    const firstReq = httpMock.expectOne('/v2/observability/filters/resolve');
    firstReq.flush({
      entries: [{ filterName: 'API', labels: { 'uuid-1': 'My Cool API' } }],
    });
  });

  it('should fall back to original conditions when backend fails', done => {
    const conditions: FilterCondition[] = [{ field: 'API', label: 'API', operator: 'EQ', values: ['uuid-1'] }];

    resolver.resolveLabels(conditions).subscribe(result => {
      expect(result).toEqual(conditions);
      done();
    });

    const req = httpMock.expectOne('/v2/observability/filters/resolve');
    req.error(new ProgressEvent('error'));
  });
});
