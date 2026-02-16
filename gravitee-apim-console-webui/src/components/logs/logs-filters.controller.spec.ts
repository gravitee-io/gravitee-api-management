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

import LogsFiltersController from './logs-filters.controller';

import { ApiService } from '../../services/api.service';
import ApplicationService from '../../services/application.service';

describe('LogsFiltersController', () => {
  let controller: LogsFiltersController;
  let $scope: any;
  let $timeout: any;
  let $log: any;
  let ApiServiceMock: jest.Mocked<ApiService>;
  let ApplicationServiceMock: jest.Mocked<ApplicationService>;
  let ngRouter: any;
  let activatedRoute: any;
  let metadata: any;
  let onFiltersChange: any;

  beforeEach(() => {
    // Setup mocks
    $scope = {
      logsFiltersForm: {
        $setPristine: jest.fn(),
      },
    };

    $timeout = jest.fn().mockImplementation(fn => fn());

    $log = {
      error: jest.fn(),
    };

    ApiServiceMock = {
      searchApis: jest.fn(),
      getSubscribers: jest.fn(),
      listByIdIn: jest.fn(),
    } as any;

    ApplicationServiceMock = {
      getSubscribedAPI: jest.fn(),
      searchPage: jest.fn(),
      listByIdIn: jest.fn(),
    } as any;

    ngRouter = {
      navigate: jest.fn(),
    };

    activatedRoute = {
      snapshot: {
        queryParams: {},
        params: {},
      },
    };

    metadata = {
      applications: [],
      apis: [],
      plans: [],
      tenants: [],
    };

    onFiltersChange = jest.fn();

    controller = new LogsFiltersController($scope, $timeout, $log, ApiServiceMock, ApplicationServiceMock, ngRouter);

    // Set up controller properties (using type assertion to access private properties)
    (controller as any).onFiltersChange = onFiltersChange;
    (controller as any).metadata = metadata;
    (controller as any).activatedRoute = activatedRoute;
    (controller as any).api = null;
  });

  describe('$onInit', () => {
    it('should initialize with default display mode when no query params', () => {
      activatedRoute.snapshot.queryParams.q = null;
      (controller as any).context = 'api';

      controller.$onInit();

      // displayMode is only set when there's a query param
      expect((controller as any).displayMode).toBeUndefined();
      expect(controller.filters).toEqual({
        api: [],
        application: [],
        status: [],
      });
    });

    it('should decode query filters when query params exist', () => {
      activatedRoute.snapshot.queryParams.q = 'api:api-id AND method:GET AND status:200';
      (controller as any).context = 'api';
      controller.filters.status = []; // Initialize status array

      controller.$onInit();

      expect(controller.filters.api).toEqual(['api-id']);
      expect(controller.filters.method).toEqual(['GET']);
      // status now correctly pushes string values, not nested arrays
      expect(controller.filters.status).toEqual(['200']);
    });

    it('should set display mode based on query filters', () => {
      activatedRoute.snapshot.queryParams.q = '_exists_:endpoint';
      (controller as any).context = 'api';

      controller.$onInit();

      expect((controller as any).displayMode).toEqual((controller as any).displayModes[1]);
    });

    it('should add Unknown application for platform context', () => {
      activatedRoute.snapshot.queryParams.q = null;
      (controller as any).context = 'platform';

      controller.$onInit();

      expect(metadata.applications).toContainEqual({
        id: '1',
        name: 'Unknown application',
      });
    });

    it('should add Unknown application for api context', () => {
      activatedRoute.snapshot.queryParams.q = null;
      (controller as any).context = 'api';

      controller.$onInit();

      expect(metadata.applications).toContainEqual({
        id: '1',
        name: 'Unknown application',
      });
    });

    it('should add Unknown API for platform context', () => {
      activatedRoute.snapshot.queryParams.q = null;
      (controller as any).context = 'platform';

      controller.$onInit();

      expect(metadata.apis).toContainEqual({
        id: '1',
        name: 'Unknown API',
      });
    });

    it('should add Unknown API for application context', () => {
      activatedRoute.snapshot.queryParams.q = null;
      (controller as any).context = 'application';

      controller.$onInit();

      expect(metadata.apis).toContainEqual({
        id: '1',
        name: 'Unknown API',
      });
    });

    it('should not add Unknown entities for other contexts', () => {
      activatedRoute.snapshot.queryParams.q = null;
      (controller as any).context = 'other';
      const initialAppsLength = metadata.applications.length;
      const initialApisLength = metadata.apis.length;

      controller.$onInit();

      expect(metadata.applications.length).toBe(initialAppsLength);
      expect(metadata.apis.length).toBe(initialApisLength);
    });
  });

  describe('search', () => {
    it('should build query and navigate with query params', () => {
      controller.filters = {
        api: ['api-1'],
        method: ['GET'],
      };

      controller.search();

      expect(ngRouter.navigate).toHaveBeenCalledWith([], {
        relativeTo: activatedRoute,
        queryParams: {
          q: expect.stringContaining('api:api-1'),
        },
        queryParamsHandling: 'merge',
      });
      expect($timeout).toHaveBeenCalled();
      expect(onFiltersChange).toHaveBeenCalled();
    });

    it('should call onFiltersChange with built query', () => {
      controller.filters = {
        status: [200],
      };

      controller.search();

      expect($timeout).toHaveBeenCalled();
      expect(onFiltersChange).toHaveBeenCalledWith({ filters: expect.any(String) });
    });
  });

  describe('clearFilters', () => {
    it('should reset form and clear filters', () => {
      controller.filters = {
        api: ['api-1'],
        method: ['GET'],
      };
      const searchSpy = jest.spyOn(controller, 'search');

      controller.clearFilters();

      expect($scope.logsFiltersForm.$setPristine).toHaveBeenCalled();
      expect(controller.filters).toEqual({});
      expect(searchSpy).toHaveBeenCalled();
    });

    it('should call search after clearing', () => {
      const searchSpy = jest.spyOn(controller, 'search');
      controller.clearFilters();

      expect(searchSpy).toHaveBeenCalled();
    });
  });

  describe('hasFilters', () => {
    it('should return false when filters are empty', () => {
      controller.filters = {};

      expect(controller.hasFilters()).toBe(false);
    });

    it('should return false when filters have empty arrays', () => {
      controller.filters = {
        api: [],
        application: [],
      };

      expect(controller.hasFilters()).toBe(false);
    });

    it('should return true when filters have values', () => {
      controller.filters = {
        api: ['api-1'],
      };

      expect(controller.hasFilters()).toBe(true);
    });

    it('should return true when filters have string values', () => {
      controller.filters = {
        uri: '/test',
      };

      expect(controller.hasFilters()).toBe(true);
    });
  });

  describe('updateDisplayMode', () => {
    it('should remove all display mode filters and set new one', () => {
      controller.filters = {
        _exists_: 'old-value',
        '!_exists_': 'old-value',
        api: ['api-1'],
      };
      (controller as any).displayMode = (controller as any).displayModes[1]; // _exists_: endpoint

      controller.updateDisplayMode();

      // The method removes all display mode fields, then sets the new one if field exists
      // Since displayModes[1] has field '_exists_', it will set filters._exists_ = 'endpoint'
      expect(controller.filters._exists_).toBe('endpoint');
      expect(controller.filters['!_exists_']).toBeUndefined();
      expect(controller.filters.api).toEqual(['api-1']); // Non-display-mode filters remain
    });

    it('should set display mode filter when field exists', () => {
      (controller as any).displayMode = (controller as any).displayModes[1];
      controller.filters = {};

      controller.updateDisplayMode();

      expect(controller.filters._exists_).toBe('endpoint');
    });

    it('should not set filter when display mode has no field', () => {
      (controller as any).displayMode = (controller as any).displayModes[0];
      controller.filters = {};

      controller.updateDisplayMode();

      expect(controller.filters._exists_).toBeUndefined();
      expect(controller.filters['!_exists_']).toBeUndefined();
    });
  });

  describe('decodeQueryFilters', () => {
    it('should decode api filter', () => {
      const query = 'api:api-1';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.api).toEqual(['api-1']);
    });

    it('should decode multiple api filters with OR', () => {
      // flatMap splits OR conditions, so each filter is processed separately
      // Both values should be accumulated in the array
      const query = 'api:api-1 OR api:api-2';
      controller['decodeQueryFilters'](query);

      // Both values should be accumulated
      expect(controller.filters.api).toEqual(['api-1', 'api-2']);
    });

    it('should decode application filter', () => {
      const query = 'application:app-1';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.application).toEqual(['app-1']);
    });

    it('should decode uri filter', () => {
      const query = 'uri:/test/path*';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.uri).toBe('/test/path');
    });

    it('should decode path filter with api context', () => {
      (controller as any).api = {
        proxy: {
          virtual_hosts: [
            {
              path: '/api',
            },
          ],
        },
      };
      const query = 'path:"/test"';
      controller['decodeQueryFilters'](query);

      // The replace only removes escaped quotes (\"), not regular quotes
      // So the quotes remain in the result
      expect(controller.filters.uri).toBe('/api"/test"');
    });

    it('should decode path filter without api context', () => {
      (controller as any).api = null;
      const query = 'path:"/test"';
      controller['decodeQueryFilters'](query);

      // The replace only removes escaped quotes (\"), not regular quotes
      expect(controller.filters.uri).toBe('"/test"');
    });

    it('should decode method filter', () => {
      const query = 'method:GET';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.method).toEqual(['GET']);
    });

    it('should decode status filter', () => {
      const query = 'status:200';
      controller.filters.status = [];
      controller['decodeQueryFilters'](query);

      // status now correctly pushes string values, not nested arrays
      expect(controller.filters.status).toEqual(['200']);
    });

    it('should decode response-time filter', () => {
      const query = 'response-time:[0 TO 100]';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.responseTime).toEqual(['[0 TO 100]']);
    });

    it('should decode _id filter', () => {
      const query = '_id:12345';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.id).toBe('12345');
    });

    it('should decode transaction filter', () => {
      const query = 'transaction:tx-123';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.transaction).toBe('tx-123');
    });

    it('should decode plan filter', () => {
      const query = 'plan:plan-1';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.plan).toEqual(['plan-1']);
    });

    it('should decode tenant filter', () => {
      const query = 'tenant:tenant-1';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.tenant).toEqual(['tenant-1']);
    });

    it('should decode _exists_ filter', () => {
      const query = '_exists_:endpoint';
      controller['decodeQueryFilters'](query);

      expect(controller.filters._exists_).toEqual(['endpoint']);
    });

    it('should decode !_exists_ filter', () => {
      const query = '!_exists_:endpoint';
      controller['decodeQueryFilters'](query);

      expect(controller.filters['!_exists_']).toEqual(['endpoint']);
    });

    it('should decode body filter and remove wildcards', () => {
      const query = 'body:*test*';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.body).toBe('test');
    });

    it('should decode endpoint filter', () => {
      const query = 'endpoint:http://example.com*';
      controller['decodeQueryFilters'](query);

      expect(controller.filters.endpoint).toBe('http://example.com');
    });

    it('should decode remote-address filter', () => {
      const query = 'remote-address:192.168.1.1';
      controller['decodeQueryFilters'](query);

      expect(controller.filters['remote-address']).toEqual(['192.168.1.1']);
    });

    it('should decode host filter', () => {
      const query = 'host:"example.com"';
      controller['decodeQueryFilters'](query);

      // The replace only removes escaped quotes (\"), not regular quotes
      expect(controller.filters.host).toBe('"example.com"');
    });

    it('should log error for unknown filter', () => {
      const query = 'unknown:value';
      controller['decodeQueryFilters'](query);

      expect($log.error).toHaveBeenCalledWith('unknown filter: ', 'unknown');
    });

    it('should decode complex query with AND and OR', () => {
      const query = 'api:api-1 AND (method:GET OR method:POST) AND status:200';
      controller.filters.status = [];
      controller['decodeQueryFilters'](query);

      expect(controller.filters.api).toContain('api-1');
      expect($log.error).not.toHaveBeenCalled();
    });
  });

  describe('buildQuery', () => {
    it('should build simple query', () => {
      const filters = {
        api: ['api-1'],
      };

      const query = controller['buildQuery'](filters);

      expect(query).toContain('api:api-1');
    });

    it('should build query with AND separator for multiple filters', () => {
      const filters = {
        api: ['api-1'],
        method: ['GET'],
      };

      const query = controller['buildQuery'](filters);

      expect(query).toContain('api:api-1');
      expect(query).toContain('AND');
      expect(query).toContain('method:GET');
    });

    it('should add leading slash to uri if missing', () => {
      const filters = {
        uri: 'test/path',
      };

      const query = controller['buildQuery'](filters);

      // The buildQuery escapes forward slashes, so / becomes \\/
      expect(query).toContain('uri:\\\\/test\\\\/path*');
    });

    it('should add trailing asterisk to uri if missing', () => {
      const filters = {
        uri: '/test/path',
      };

      const query = controller['buildQuery'](filters);

      // The buildQuery escapes forward slashes, so / becomes \\/
      expect(query).toContain('uri:\\\\/test\\\\/path*');
    });

    it('should escape special characters in uri', () => {
      const filters = {
        uri: '/test+path',
      };

      const query = controller['buildQuery'](filters);

      expect(query).toContain('\\+');
    });

    it('should wrap body filter with wildcards', () => {
      const filters = {
        body: 'test',
      };

      const query = controller['buildQuery'](filters);

      expect(query).toContain('body:*test*');
    });

    it('should convert multiple values to OR format', () => {
      const filters = {
        api: ['api-1', 'api-2'],
      };

      const query = controller['buildQuery'](filters);

      expect(query).toContain('(api-1 OR api-2)');
    });

    it('should map field names correctly', () => {
      const filters = {
        responseTime: ['[0 TO 100]'],
        id: '123',
      };

      const query = controller['buildQuery'](filters);

      expect(query).toContain('response-time:');
      expect(query).toContain('_id:');
    });

    it('should ignore empty arrays', () => {
      const filters = {
        api: [],
        method: ['GET'],
      };

      const query = controller['buildQuery'](filters);

      expect(query).not.toContain('api:');
      expect(query).toContain('method:GET');
    });

    it('should handle single value arrays', () => {
      const filters = {
        api: ['api-1'],
      };

      const query = controller['buildQuery'](filters);

      expect(query).toContain('api:api-1');
      expect(query).not.toContain('(');
    });
  });

  describe('searchApis', () => {
    it('should search subscribed APIs for application context', async () => {
      (controller as any).context = 'application';
      activatedRoute.snapshot.params.applicationId = 'app-1';
      const mockApis = [
        { id: 'api-1', name: 'API 1' },
        { id: 'api-2', name: 'API 2' },
      ];
      ApplicationServiceMock.getSubscribedAPI.mockResolvedValue({
        data: mockApis,
      } as any);

      const result = await controller.searchApis('API');

      expect(ApplicationServiceMock.getSubscribedAPI).toHaveBeenCalledWith('app-1');
      expect(result).toEqual(mockApis);
    });

    it('should filter subscribed APIs by search term', async () => {
      (controller as any).context = 'application';
      activatedRoute.snapshot.params.applicationId = 'app-1';
      const mockApis = [
        { id: 'api-1', name: 'API One' },
        { id: 'api-2', name: 'API Two' },
        { id: 'api-3', name: 'Other API' },
      ];
      ApplicationServiceMock.getSubscribedAPI.mockResolvedValue({
        data: mockApis,
      } as any);

      const result = await controller.searchApis('One');

      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('API One');
    });

    it('should limit results to 10 for application context', async () => {
      (controller as any).context = 'application';
      activatedRoute.snapshot.params.applicationId = 'app-1';
      const mockApis = Array.from({ length: 15 }, (_, i) => ({
        id: `api-${i}`,
        name: `API ${i}`,
      }));
      ApplicationServiceMock.getSubscribedAPI.mockResolvedValue({
        data: mockApis,
      } as any);

      const result = await controller.searchApis('');

      expect(result).toHaveLength(10);
    });

    it('should use ApiService for non-application context', async () => {
      (controller as any).context = 'platform';
      const mockApis = [{ id: 'api-1', name: 'API 1' }];
      ApiServiceMock.searchApis.mockResolvedValue({
        data: { data: mockApis },
      } as any);

      const result = await controller.searchApis('API');

      expect(ApiServiceMock.searchApis).toHaveBeenCalledWith('API', 1, null, null, 10);
      expect(result).toEqual(mockApis);
    });
  });

  describe('searchApplications', () => {
    it('should search subscribers for api context', async () => {
      (controller as any).context = 'api';
      (controller as any).api = { id: 'api-1' };
      const mockApps = [{ id: 'app-1', name: 'App 1' }];
      ApiServiceMock.getSubscribers.mockResolvedValue({
        data: mockApps,
      } as any);

      const result = await controller.searchApplications('App');

      expect(ApiServiceMock.getSubscribers).toHaveBeenCalledWith('api-1', 'App', 1, 10);
      expect(result).toEqual(mockApps);
    });

    it('should use ApplicationService for non-api context', async () => {
      (controller as any).context = 'platform';
      const mockApps = [{ id: 'app-1', name: 'App 1' }];
      ApplicationServiceMock.searchPage.mockResolvedValue({
        data: { data: mockApps },
      } as any);

      const result = await controller.searchApplications('App');

      expect(ApplicationServiceMock.searchPage).toHaveBeenCalledWith('App', 1, 10);
      expect(result).toEqual(mockApps);
    });
  });

  describe('initApisSelector', () => {
    it('should load APIs by IDs', async () => {
      controller.filters.api = ['api-1', 'api-2'];
      const mockApis = [
        { id: 'api-1', name: 'API 1' },
        { id: 'api-2', name: 'API 2' },
      ];
      ApiServiceMock.listByIdIn.mockResolvedValue({
        data: mockApis,
      } as any);

      const result = await controller.initApisSelector();

      expect(ApiServiceMock.listByIdIn).toHaveBeenCalledWith(['api-1', 'api-2']);
      expect(result).toEqual(mockApis);
    });
  });

  describe('initApplicationsSelector', () => {
    it('should load applications by IDs', async () => {
      controller.filters.application = ['app-1', 'app-2'];
      const mockApps = [
        { id: 'app-1', name: 'App 1' },
        { id: 'app-2', name: 'App 2' },
      ];
      ApplicationServiceMock.listByIdIn.mockResolvedValue({
        data: mockApps,
      } as any);

      const result = await controller.initApplicationsSelector();

      expect(ApplicationServiceMock.listByIdIn).toHaveBeenCalledWith(['app-1', 'app-2']);
      expect(result).toEqual(mockApps);
    });
  });

  describe('constants', () => {
    it('should have correct methods mapping', () => {
      expect(controller.methods).toEqual({
        0: 'OTHER',
        1: 'CONNECT',
        2: 'DELETE',
        3: 'GET',
        4: 'HEAD',
        5: 'OPTIONS',
        6: 'PATCH',
        7: 'POST',
        8: 'PUT',
        9: 'TRACE',
      });
    });

    it('should have response times mapping', () => {
      expect(controller.responseTimes['[0 TO 100]']).toBe('0 to 100ms');
      expect(controller.responseTimes['[5000 TO *]']).toBe('> 5000ms');
    });

    it('should have HTTP status codes mapping', () => {
      expect(controller.httpStatus[200]).toBe('OK');
      expect(controller.httpStatus[404]).toBe('NOT FOUND');
      expect(controller.httpStatus[500]).toBe('INTERNAL SERVER ERROR');
    });
  });
});
