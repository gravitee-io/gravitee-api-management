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
import { of } from 'rxjs';

import { ApiMetrics } from './api.metrics';

import { Scope, Tuple } from '../alert';

describe('ApiMetrics', () => {
  describe('API loader', () => {
    let mockInjector: any;
    let mockApiV2Service: any;
    let mockApplicationService: any;

    beforeEach(() => {
      mockApiV2Service = {
        getAll: jest.fn(),
      };

      mockApplicationService = {
        getSubscribedAPI: jest.fn(),
      };

      mockInjector = {
        get: jest.fn((token: string) => {
          if (token === 'ngApiV2Service') return mockApiV2Service;
          if (token === 'ApplicationService') return mockApplicationService;
          return null;
        }),
      };
    });

    it('should load all APIs (v2 and v4) via ngApiV2Service when scope is ENVIRONMENT', () => {
      mockApiV2Service.getAll.mockReturnValue(
        of([
          { id: 'api-v2-id', name: 'My V2 API' },
          { id: 'api-v4-id', name: 'My V4 API' },
        ]),
      );

      const result = ApiMetrics.API.loader(Scope.ENVIRONMENT, null, mockInjector);

      expect(mockApiV2Service.getAll).toHaveBeenCalledTimes(1);
      expect(result).toHaveLength(2);
      expect(result[0]).toEqual(new Tuple('api-v2-id', 'My V2 API'));
      expect(result[1]).toEqual(new Tuple('api-v4-id', 'My V4 API'));
    });

    it('should only use the final emission when getAll emits multiple accumulated pages', () => {
      // getAll() uses scan() internally: each emission contains the full accumulated list.
      // last() ensures only the final (complete) list is used.
      const page1 = [{ id: 'api-1', name: 'API 1' }];
      const page1and2 = [
        { id: 'api-1', name: 'API 1' },
        { id: 'api-2', name: 'API 2' },
      ];
      mockApiV2Service.getAll.mockReturnValue(of(page1, page1and2));

      const result = ApiMetrics.API.loader(Scope.ENVIRONMENT, null, mockInjector);

      expect(result).toHaveLength(2);
      expect(result[0]).toEqual(new Tuple('api-1', 'API 1'));
      expect(result[1]).toEqual(new Tuple('api-2', 'API 2'));
    });

    it('should load subscribed APIs via ApplicationService when scope is APPLICATION', () => {
      mockApplicationService.getSubscribedAPI.mockResolvedValue({
        data: [{ id: 'sub-api-id', name: 'Subscribed API' }],
      });

      ApiMetrics.API.loader(Scope.APPLICATION, 'app-id', mockInjector);

      expect(mockApplicationService.getSubscribedAPI).toHaveBeenCalledWith('app-id');
      expect(mockApiV2Service.getAll).not.toHaveBeenCalled();
    });

    it('should not call any service when scope is API', () => {
      ApiMetrics.API.loader(Scope.API, 'api-id', mockInjector);

      expect(mockApiV2Service.getAll).not.toHaveBeenCalled();
      expect(mockApplicationService.getSubscribedAPI).not.toHaveBeenCalled();
    });
  });
});
