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
import AnalyticsDashboardComponentAjs from './analytics-dashboard.component.ajs';

describe('AnalyticsDashboardControllerAjs', () => {
  let controller: any;
  let eventServiceMock: { search: jest.Mock };
  let analyticsServiceMock: { getQueryFilters: jest.Mock; parseQueryFilters: jest.Mock; analytics: jest.Mock };
  let dashboardServiceMock: { list: jest.Mock };
  let ngRouterMock: { navigate: jest.Mock };
  let activatedRouteMock: { snapshot: { queryParams: Record<string, string> } };

  const firstDashboard = {
    id: 'dashboard-1',
    enabled: true,
    definition: JSON.stringify([{ id: 'widget-1' }]),
  };
  const secondDashboard = {
    id: 'dashboard-2',
    enabled: true,
    definition: JSON.stringify([{ id: 'widget-2' }]),
  };

  beforeEach(() => {
    eventServiceMock = {
      search: jest.fn().mockResolvedValue({ data: [{ id: 'event-1' }] }),
    };
    analyticsServiceMock = {
      getQueryFilters: jest.fn().mockReturnValue(null),
      parseQueryFilters: jest.fn().mockReturnValue(null),
      analytics: jest.fn(),
    };
    dashboardServiceMock = {
      list: jest.fn().mockResolvedValue({ data: [firstDashboard, secondDashboard] }),
    };
    ngRouterMock = {
      navigate: jest.fn(),
    };
    activatedRouteMock = {
      snapshot: {
        queryParams: {},
      },
    };

    const ControllerClass = AnalyticsDashboardComponentAjs.controller as new (...args: unknown[]) => unknown;
    controller = new ControllerClass(
      eventServiceMock,
      analyticsServiceMock,
      {},
      {},
      { eventsFetchData: false },
      {},
      dashboardServiceMock,
      ngRouterMock,
    );
    controller.activatedRoute = activatedRouteMock;
  });

  const initDashboards = async () => {
    controller.$onInit();
    await dashboardServiceMock.list.mock.results[0].value;
  };

  describe('$onInit', () => {
    it('should store the first dashboard id and bind searchEvents', async () => {
      await initDashboards();

      expect(controller.firstDashboardId).toEqual('dashboard-1');
      expect(controller.dashboard).toEqual(firstDashboard);
      expect(typeof controller.searchEvents).toBe('function');
    });
  });

  describe('onTimeframeChange', () => {
    beforeEach(async () => {
      await initDashboards();
      controller.dashboard = { id: controller.firstDashboardId };
      controller.lastFrom = 1_000;
      controller.lastTo = 2_000;
      eventServiceMock.search.mockClear();
    });

    it('should search events when the current dashboard is the first one', async () => {
      controller.onTimeframeChange({ from: 1_000, to: 2_000 });
      await Promise.resolve();

      expect(controller.lastFrom).toEqual(1_000);
      expect(controller.lastTo).toEqual(2_000);
      expect(eventServiceMock.search).toHaveBeenCalled();
    });

    it('should not search events when the current dashboard is not the first one', async () => {
      controller.dashboard = { id: 'dashboard-2' };

      controller.onTimeframeChange({ from: 1_000, to: 2_000 });
      await Promise.resolve();

      expect(eventServiceMock.search).not.toHaveBeenCalled();
    });
  });

  describe('searchEvents', () => {
    beforeEach(async () => {
      await initDashboards();
      controller.lastFrom = 1_000;
      controller.lastTo = 2_000;
    });

    it('should search platform events without API filter when no query filters are set', async () => {
      analyticsServiceMock.getQueryFilters.mockReturnValue(null);

      controller.searchEvents();
      await Promise.resolve();

      expect(analyticsServiceMock.getQueryFilters).toHaveBeenCalledWith(activatedRouteMock);
      expect(eventServiceMock.search).toHaveBeenCalledWith(
        ['START_API', 'STOP_API', 'PUBLISH_API', 'UNPUBLISH_API'],
        '',
        1_000,
        2_000,
        0,
        10,
      );
      expect(controller.events).toEqual([{ id: 'event-1' }]);
    });

    it('should search platform events with API ids from query filters', async () => {
      analyticsServiceMock.getQueryFilters.mockReturnValue({ api: ['api-1', 'api-2'] });

      controller.searchEvents();
      await Promise.resolve();

      expect(eventServiceMock.search).toHaveBeenCalledWith(
        ['START_API', 'STOP_API', 'PUBLISH_API', 'UNPUBLISH_API'],
        'api-1,api-2',
        1_000,
        2_000,
        0,
        10,
      );
    });

    it('should parse query filters when a query string is provided', async () => {
      analyticsServiceMock.parseQueryFilters.mockReturnValue({ api: ['api-3'] });

      controller.searchEvents('(api:api-3)');
      await Promise.resolve();

      expect(analyticsServiceMock.parseQueryFilters).toHaveBeenCalledWith('(api:api-3)');
      expect(analyticsServiceMock.getQueryFilters).not.toHaveBeenCalled();
      expect(eventServiceMock.search).toHaveBeenCalledWith(
        ['START_API', 'STOP_API', 'PUBLISH_API', 'UNPUBLISH_API'],
        'api-3',
        1_000,
        2_000,
        0,
        10,
      );
    });
  });

  describe('onFilterChange', () => {
    beforeEach(async () => {
      await initDashboards();
      controller.dashboard = { id: controller.firstDashboardId };
      controller.lastFrom = 1_000;
      controller.lastTo = 2_000;
      eventServiceMock.search.mockClear();
    });

    it('should search events when the current dashboard is the first one', async () => {
      analyticsServiceMock.parseQueryFilters.mockReturnValue({ api: ['api-1'] });

      controller.onFilterChange('(api:api-1)');
      await Promise.resolve();

      expect(analyticsServiceMock.parseQueryFilters).toHaveBeenCalledWith('(api:api-1)');
      expect(eventServiceMock.search).toHaveBeenCalled();
    });

    it('should not search events when the current dashboard is not the first one', async () => {
      controller.dashboard = { id: 'dashboard-2' };

      controller.onFilterChange('(api:api-1)');
      await Promise.resolve();

      expect(eventServiceMock.search).not.toHaveBeenCalled();
    });
  });
});
