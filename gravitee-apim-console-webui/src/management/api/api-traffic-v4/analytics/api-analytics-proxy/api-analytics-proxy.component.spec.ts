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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { ApiAnalyticsProxyComponent } from './api-analytics-proxy.component';
import { ApiAnalyticsProxyHarness } from './api-analytics-proxy.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeAnalyticsHistogram } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram.fixture';
import { fakeGroupByResponse } from '../../../../../entities/management-api-v2/analytics/analyticsGroupBy.fixture';
import { fakeAnalyticsStatsResponse } from '../../../../../entities/management-api-v2/analytics/analyticsStats.fixture';
import { fakePagedResult, fakePlanV4, PlanV4 } from '../../../../../entities/management-api-v2';

describe('ApiAnalyticsProxyComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiAnalyticsProxyComponent>;
  let componentHarness: ApiAnalyticsProxyHarness;
  let httpTestingController: HttpTestingController;

  const initComponent = async (queryParams = {}) => {
    TestBed.configureTestingModule({
      imports: [ApiAnalyticsProxyComponent, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: queryParams,
            },
            queryParams: of(queryParams),
            params: of({ apiId: API_ID }),
          },
        },
      ],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiAnalyticsProxyComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsProxyHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('GIVEN an API with analytics enabled', () => {
    beforeEach(async () => {
      await initComponent();
      // Handle all initial requests
      handleAllRequests();
    });

    it('should display analytics widgets in empty state', async () => {
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeFalsy();
    });

    it('should refresh when filters are applied', async () => {
      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.clickRefresh();
      handleAllRequests();
    });
  });

  describe('Query parameters', () => {
    const plan1 = fakePlanV4({ id: '1', name: 'plan 1' });
    const plan2 = fakePlanV4({ id: '2', name: 'plan 2' });

    const mockCustomFromTimestamp = 1640908800000;
    const mockCustomToTimestamp = 1640995200000;

    it('should use default time range when no query params provided', async () => {
      await initComponent();
      handleAllRequests();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValue = await filtersBar.getSelectedPeriod();

      expect(selectedValue).toEqual('Last day');
    });

    it('should use custom time range from query params', async () => {
      await initComponent({ period: '1M' });
      handleAllRequests();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValue = await filtersBar.getSelectedPeriod();

      expect(selectedValue).toEqual('Last month');
    });

    it('should use and select plans from query params', async () => {
      await initComponent({ plans: '1' });

      expectPlanList([plan1, plan2]);
      handleAllRequests();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValues = await filtersBar.getSelectedPlans();

      expect(selectedValues).toEqual(['1']);
    });

    it('should update the URL query params when a plan is selected', async () => {
      await initComponent();

      expectPlanList([plan1, plan2]);
      handleAllRequests();

      const router = TestBed.inject(Router);
      const routerSpy = jest.spyOn(router, 'navigate');

      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.selectPlan('plan 2');

      expect(routerSpy).toHaveBeenCalledWith([], {
        queryParams: {
          period: '1d',
          plans: '2',
        },
        queryParamsHandling: 'replace',
      });
    });

    it('should select http statuses from query params', async () => {
      await initComponent({ httpStatuses: '200' });

      handleAllRequests();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValues = await filtersBar.getSelectedHttpStatuses();

      expect(selectedValues).toEqual(['200']);
    });

    it('should update the URL query params when a httpStatus is selected', async () => {
      await initComponent();

      handleAllRequests();

      const router = TestBed.inject(Router);
      const routerSpy = jest.spyOn(router, 'navigate');

      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.selectHttpStatus('404');

      expect(routerSpy).toHaveBeenCalledWith([], {
        queryParams: {
          period: '1d',
          httpStatuses: '404',
        },
        queryParamsHandling: 'replace',
      });
    });

    it('should navigate to logs with custom timestamp query parameters', async () => {
      const mockQueryParams = {
        period: 'custom',
        from: mockCustomFromTimestamp.toString(),
        to: mockCustomToTimestamp.toString(),
        httpStatuses: '200,404',
        plans: 'plan-1,plan-2',
        applications: 'app-1,app-2',
      };

      await initComponent(mockQueryParams);
      handleAllRequests();

      const router = TestBed.inject(Router);
      const routerSpy = jest.spyOn(router, 'navigate');

      const component = fixture.componentInstance;

      component.navigateToLogs();

      expect(routerSpy).toHaveBeenCalledWith(['../runtime-logs'], {
        relativeTo: expect.anything(),
        queryParams: {
          from: mockCustomFromTimestamp,
          to: mockCustomToTimestamp,
          statuses: '200,404',
          planIds: 'plan-1,plan-2',
          applicationIds: 'app-1,app-2',
        },
      });
    });

    it('should navigate to logs with predefined time period query parameters', async () => {
      const mockQueryParams = {
        period: '1d',
        httpStatuses: '200,404',
        plans: 'plan-1,plan-2',
        applications: 'app-1,app-2',
      };

      await initComponent(mockQueryParams);
      handleAllRequests();

      const router = TestBed.inject(Router);
      const routerSpy = jest.spyOn(router, 'navigate');

      const component = fixture.componentInstance;

      component.navigateToLogs();

      expect(routerSpy).toHaveBeenCalledWith(['../runtime-logs'], {
        relativeTo: expect.anything(),
        queryParams: {
          from: expect.any(Number),
          to: expect.any(Number),
          statuses: '200,404',
          planIds: 'plan-1,plan-2',
          applicationIds: 'app-1,app-2',
        },
      });
    });
  });

  describe('Backend API calls based on query parameters', () => {
    it('should make backend calls with default time range when no query params provided', async () => {
      await initComponent();

      // Verify that backend calls are made with default time range (1d)
      const requests = httpTestingController.match(req => req.url.includes('/analytics'));
      expect(requests.length).toBeGreaterThan(0);

      // Check that all requests include the default time range parameters
      requests.forEach(req => {
        expect(req.request.url).toContain('type=');
        expect(req.request.url).toContain('from=');
        expect(req.request.url).toContain('to=');
        expect(req.request.url).toContain('interval=');
      });

      handleAllRequests();
    });

    it('should make backend calls with custom period from query params', async () => {
      await initComponent({ period: 'custom', from: '1', to: '2' });

      // Verify that backend calls are made with 7d time range
      const requests = httpTestingController.match(req => req.url.includes('/analytics'));
      expect(requests.length).toBeGreaterThan(0);

      // Check that requests include the correct time range
      requests
        .filter(request => !request.cancelled)
        .forEach(req => {
          expect(req.request.url).toContain('from=1');
          expect(req.request.url).toContain('to=2');
          expect(req.request.url).toContain('interval=0');
        });

      handleAllRequests();
    });

    it('should make different backend calls for different analytics types', async () => {
      await initComponent({ period: '1d' });

      // Get all analytics requests
      const requests = httpTestingController.match(req => req.url.includes('/analytics'));

      // Should have requests for different analytics types
      const statsRequests = requests.filter(req => req.request.url.includes('type=STATS'));
      const groupByRequests = requests.filter(req => req.request.url.includes('type=GROUP_BY'));
      const histogramRequests = requests.filter(req => req.request.url.includes('type=HISTOGRAM'));

      expect(statsRequests.length).toBeGreaterThan(0);
      expect(groupByRequests.length).toBeGreaterThan(0);
      expect(histogramRequests.length).toBeGreaterThan(0);

      // Verify each type has the correct parameters
      statsRequests.forEach(req => {
        expect(req.request.url).toContain('type=STATS');
        expect(req.request.url).toContain('from=');
        expect(req.request.url).toContain('to=');
        expect(req.request.url).toContain('interval=2880000');
      });

      groupByRequests.forEach(req => {
        expect(req.request.url).toContain('type=GROUP_BY');
        expect(req.request.url).toContain('from=');
        expect(req.request.url).toContain('to=');
        expect(req.request.url).toContain('interval=2880000');
        expect(req.request.url).toContain('field=');
      });

      histogramRequests.forEach(req => {
        expect(req.request.url).toContain('type=HISTOGRAM');
        expect(req.request.url).toContain('from=');
        expect(req.request.url).toContain('to=');
        expect(req.request.url).toContain('interval=2880000');
        expect(req.request.url).toContain('aggregations=');
      });

      handleAllRequests();
    });

    it('should include API ID in all backend calls', async () => {
      await initComponent({ period: '1d' });

      const requests = httpTestingController.match(req => req.url.includes('/analytics'));
      expect(requests.length).toBeGreaterThan(0);

      // Verify all requests include the API ID
      requests.forEach(req => {
        expect(req.request.url).toContain(`/apis/${API_ID}/analytics`);
      });

      handleAllRequests();
    });

    it('should handle multiple concurrent requests with same parameters', async () => {
      await initComponent({ period: '1d' });

      // Should make multiple requests for different widgets
      const requests = httpTestingController.match(req => req.url.includes('/analytics'));
      expect(requests.length).toBeGreaterThan(5); // Multiple widgets

      // All requests should have the same time range parameters
      requests.forEach(req => {
        // Extract time parameters from URL
        const fromMatch = req.request.url.match(/from=(\d+)/);
        const toMatch = req.request.url.match(/to=(\d+)/);
        const intervalMatch = req.request.url.match(/interval=(\d+)/);

        expect(fromMatch).toBeTruthy();
        expect(toMatch).toBeTruthy();
        expect(intervalMatch).toBeTruthy();
      });

      handleAllRequests();
    });
  });

  function handleAllRequests() {
    // Handle all pending requests
    const requests = httpTestingController.match(() => true);
    requests.forEach(request => {
      if (request.request.url.includes('type=HISTOGRAM')) {
        request.flush(fakeAnalyticsHistogram());
      } else if (request.request.url.includes('type=GROUP_BY')) {
        request.flush(fakeGroupByResponse());
      } else if (request.request.url.includes('type=STATS')) {
        request.flush(fakeAnalyticsStatsResponse());
      }
    });
  }

  function expectPlanList(plans: PlanV4[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999&statuses=PUBLISHED,DEPRECATED,CLOSED`,
        method: 'GET',
      })
      .flush(fakePagedResult(plans));
    fixture.detectChanges();
  }
});
