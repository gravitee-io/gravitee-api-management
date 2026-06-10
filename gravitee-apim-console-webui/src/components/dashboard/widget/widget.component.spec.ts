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

import WidgetComponent from './widget.component';

import AnalyticsService from '../../../services/analytics.service';

describe('WidgetComponent', () => {
  let controller: any;
  let eventServiceMock: { search: jest.Mock };
  let ApiServiceMock: { searchApiEvents: jest.Mock };
  let analyticsServiceMock: { getQueryFilters: jest.Mock; buildQueryParam: jest.Mock };

  const createController = () => {
    const instance: any = {};
    const $scope = {
      $on: jest.fn(() => jest.fn()),
      $broadcast: jest.fn(),
      $emit: jest.fn(),
    };

    eventServiceMock = {
      search: jest.fn().mockResolvedValue({ data: [{ id: 'event-1' }] }),
    };
    ApiServiceMock = {
      searchApiEvents: jest.fn().mockResolvedValue({ data: [{ id: 'event-2' }] }),
    };
    analyticsServiceMock = {
      getQueryFilters: jest.fn(),
      buildQueryParam: jest.fn((queryParam: string) => queryParam),
    };

    type WidgetControllerFn = ($scope: unknown, analyticsService: unknown, eventService: unknown, apiService: unknown) => void;
    const controllerFn = (WidgetComponent.controller as (string | WidgetControllerFn)[])[
      WidgetComponent.controller.length - 1
    ] as WidgetControllerFn;
    controllerFn.call(instance, $scope, analyticsServiceMock, eventServiceMock, ApiServiceMock);

    return instance;
  };

  const reloadAndWait = async () => {
    controller.reload();
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
  };

  const createLineWidget = () => ({
    chart: {
      type: 'line',
      request: {
        field: 'status',
        from: 1_000,
        to: 2_000,
      },
      service: {
        function: jest.fn().mockResolvedValue({
          data: {
            timestamp: { from: 1_000, to: 2_000 },
          },
        }),
        caller: {},
      },
    },
  });

  beforeEach(() => {
    controller = createController();
    controller.widget = createLineWidget();
    controller.activatedRoute = {
      snapshot: {
        params: {},
        queryParams: {},
      },
    };
  });

  describe('reload line chart events', () => {
    it('should search platform events without API filter when no API is selected', async () => {
      analyticsServiceMock.getQueryFilters.mockReturnValue(null);

      await reloadAndWait();

      expect(eventServiceMock.search).toHaveBeenCalledWith(['PUBLISH_API'], '', 1_000, 2_000, 0, 10);
      expect(ApiServiceMock.searchApiEvents).not.toHaveBeenCalled();
      expect(controller.results.events).toEqual([{ id: 'event-1' }]);
    });

    it('should search platform events with API ids from query filters on global analytics', async () => {
      analyticsServiceMock.getQueryFilters.mockReturnValue({ api: ['api-1'] });
      controller.activatedRoute.snapshot.queryParams.q = '(api:api-1)';

      await reloadAndWait();

      expect(eventServiceMock.search).toHaveBeenCalledWith(['PUBLISH_API'], 'api-1', 1_000, 2_000, 0, 10);
      expect(ApiServiceMock.searchApiEvents).not.toHaveBeenCalled();
      expect(controller.results.events).toEqual([{ id: 'event-1' }]);
    });

    it('should search platform events with multiple API ids from query filters', async () => {
      analyticsServiceMock.getQueryFilters.mockReturnValue({ api: ['api-1', 'api-2'] });

      await reloadAndWait();

      expect(eventServiceMock.search).toHaveBeenCalledWith(['PUBLISH_API'], 'api-1,api-2', 1_000, 2_000, 0, 10);
      expect(ApiServiceMock.searchApiEvents).not.toHaveBeenCalled();
    });

    it('should search API events when apiId route param is present', async () => {
      controller.activatedRoute.snapshot.params.apiId = 'api-1';
      analyticsServiceMock.getQueryFilters.mockReturnValue({ api: ['api-1'] });

      await reloadAndWait();

      expect(ApiServiceMock.searchApiEvents).toHaveBeenCalledWith(['PUBLISH_API'], 'api-1', 1_000, 2_000, 0, 10);
      expect(eventServiceMock.search).not.toHaveBeenCalled();
      expect(controller.results.events).toEqual([{ id: 'event-2' }]);
    });

    it('should parse API filters from analytics query params', async () => {
      const analyticsService = new AnalyticsService({} as any, {} as any);
      controller.AnalyticsService = analyticsService;
      controller.activatedRoute.snapshot.queryParams.q = '(api:api-1)';

      await reloadAndWait();

      expect(eventServiceMock.search).toHaveBeenCalledWith(['PUBLISH_API'], 'api-1', 1_000, 2_000, 0, 10);
      expect(ApiServiceMock.searchApiEvents).not.toHaveBeenCalled();
    });
  });
});
