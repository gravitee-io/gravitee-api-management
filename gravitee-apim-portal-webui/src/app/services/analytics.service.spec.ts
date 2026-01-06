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
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';

import { TranslateTestingModule } from '../test/translate-testing-module';

import { AnalyticsService } from './analytics.service';

describe('AnalyticsService', () => {
  const mockActivatedRouteSnapshotQueryParam = {
    method: ['7', '3'],
    'response-time': ['[300 TO 400]', '[400 TO 500]'],
    from: 1767654000000,
    to: 1767740400000,
    status: '200',
  };

  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [TranslateTestingModule, RouterTestingModule],
      teardown: { destroyAfterEach: false },
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: mockActivatedRouteSnapshotQueryParam,
            },
          },
        },
      ],
    }),
  );

  it('should be created', () => {
    const service: AnalyticsService = TestBed.inject(AnalyticsService);
    expect(service).toBeTruthy();
  });

  describe('buildQueryParam()', () => {
    it('should use "starts with" wildcard for uri parameter', () => {
      const resultQueryParam = AnalyticsService.buildQueryParam('/test/uri?bala=test', 'uri');
      expect(resultQueryParam).toBe('\\\\/test\\\\/uri\\\\?bala\\\\=test*');
    });

    it('should use "contains" wildcard for body parameter', () => {
      const resultQueryParam = AnalyticsService.buildQueryParam('part of body content', 'body');
      expect(resultQueryParam).toBe('*part of body content*');
    });

    it('should not escape response-time values', () => {
      const resultQueryParam = AnalyticsService.buildQueryParam('[300 to 400]', 'response-time');
      expect(resultQueryParam).toBe('[300 to 400]');
    });

    it('should use quotes for other parameters', () => {
      const resultQueryParam = AnalyticsService.buildQueryParam('exact = text * search', 'another');
      expect(resultQueryParam).toBe('\\"exact = text * search\\"');
    });

    it('should not quote the "?" wildcard', () => {
      const resultQueryParam = AnalyticsService.buildQueryParam('?', 'another');
      expect(resultQueryParam).toBe('?');
    });
  });

  describe('getQueryFromPath', () => {
    it('should group multivalued query param with parenthesis', () => {
      const service: AnalyticsService = TestBed.inject(AnalyticsService);

      const query = service.getQueryFromPath().query;
      expect(query).toBe('method:(\\"7\\" OR \\"3\\") AND response-time:([300 TO 400] OR [400 TO 500]) AND status:\\"200\\"');
    });
  });
});
