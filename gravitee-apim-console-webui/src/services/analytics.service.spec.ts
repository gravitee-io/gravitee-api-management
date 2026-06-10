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
import { ActivatedRoute } from '@angular/router';

import AnalyticsService from './analytics.service';

describe('AnalyticsService', () => {
  let analyticsService: AnalyticsService;

  beforeEach(() => {
    analyticsService = new AnalyticsService(
      {},
      {
        env: {
          baseURL: 'https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT',
          settings: {
            analytics: {
              clientTimeout: 30_000,
            },
          },
        },
      },
    );
  });

  describe('parseQueryFilters', () => {
    it('should return null when query is empty', () => {
      expect(analyticsService.parseQueryFilters()).toBeNull();
      expect(analyticsService.parseQueryFilters('')).toBeNull();
    });

    it('should parse API filters from query string', () => {
      expect(analyticsService.parseQueryFilters('(api:api-1)')).toEqual({ api: ['api-1'] });
    });

    it('should parse multiple API filters from query string', () => {
      expect(analyticsService.parseQueryFilters('(api:api-1) AND (api:api-2)')).toEqual({
        api: ['api-1', 'api-2'],
      });
    });
  });

  describe('getQueryFilters', () => {
    it('should delegate to parseQueryFilters using activated route query params', () => {
      const activatedRoute = {
        snapshot: {
          queryParams: {
            q: '(api:api-1)',
          },
        },
      } as unknown as ActivatedRoute;

      expect(analyticsService.getQueryFilters(activatedRoute)).toEqual({ api: ['api-1'] });
    });

    it('should return null when activated route has no query filter', () => {
      const activatedRoute = {
        snapshot: {
          queryParams: {},
        },
      } as unknown as ActivatedRoute;

      expect(analyticsService.getQueryFilters(activatedRoute)).toBeNull();
    });
  });
});
