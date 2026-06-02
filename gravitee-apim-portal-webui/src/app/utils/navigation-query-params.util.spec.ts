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
import { FilterApiQuery } from '../../../projects/portal-webclient-sdk/src/lib';

import { getNavigationContextQueryParams, navigateWithNavigationContext, parsePaginationParam } from './navigation-query-params.util';
import { SearchQueryParam } from './search-query-param.enum';

describe('navigation-query-params.util', () => {
  describe('getNavigationContextQueryParams', () => {
    it('should extract only navigation context params, dropping page and size', () => {
      expect(
        getNavigationContextQueryParams({
          [SearchQueryParam.API_QUERY]: FilterApiQuery.ALL,
          page: 'documentation-page-id',
          size: '12',
        }),
      ).toEqual({ [SearchQueryParam.API_QUERY]: FilterApiQuery.ALL });
    });
  });

  describe('parsePaginationParam', () => {
    it('should parse a valid page number', () => {
      expect(parsePaginationParam('3', 1)).toBe(3);
    });

    it('should return defaultValue for a non-numeric string', () => {
      expect(parsePaginationParam('documentation-page-id', 1)).toBe(1);
    });

    it('should return defaultValue for null', () => {
      expect(parsePaginationParam(null, 12)).toBe(12);
    });

    it('should return defaultValue for zero', () => {
      expect(parsePaginationParam('0', 1)).toBe(1);
    });

    it('should return defaultValue for a negative number', () => {
      expect(parsePaginationParam('-1', 1)).toBe(1);
    });
  });

  describe('navigateWithNavigationContext', () => {
    it('should preserve navigation context while dropping non-context params and merging target params', () => {
      const router = {
        url: '/catalog/api/123/doc?page=documentation-page-id&aq=ALL',
        navigate: jest.fn().mockResolvedValue(true),
        parseUrl: jest.fn().mockImplementation((url: string) => {
          if (url === '/applications') {
            return { root: { children: { primary: { segments: [{ toString: () => 'applications' }] } } }, queryParams: {} };
          }
          return {
            root: { children: { primary: { segments: [] } } },
            queryParams: { [SearchQueryParam.API_QUERY]: FilterApiQuery.ALL, page: 'documentation-page-id' },
          };
        }),
      } as any;

      navigateWithNavigationContext(router, '/applications');

      expect(router.navigate).toHaveBeenCalledWith(['applications'], {
        queryParams: { [SearchQueryParam.API_QUERY]: FilterApiQuery.ALL },
      });
    });

    it('should navigate to root when routePath resolves to /', () => {
      const router = {
        url: '/catalog?page=1',
        navigate: jest.fn().mockResolvedValue(true),
        parseUrl: jest.fn().mockImplementation((url: string) => {
          if (url === '/') {
            return { root: { children: {} }, queryParams: {} };
          }
          return {
            root: { children: { primary: { segments: [] } } },
            queryParams: { [SearchQueryParam.API_QUERY]: FilterApiQuery.ALL },
          };
        }),
      } as any;

      navigateWithNavigationContext(router, '/');

      expect(router.navigate).toHaveBeenCalledWith([''], {
        queryParams: { [SearchQueryParam.API_QUERY]: FilterApiQuery.ALL },
      });
    });
  });
});
