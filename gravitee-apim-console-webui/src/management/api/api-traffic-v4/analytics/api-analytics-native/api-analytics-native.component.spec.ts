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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { of } from 'rxjs';

import { ApiAnalyticsNativeHarness } from './api-analytics-native.component.harness';
import { ApiAnalyticsNativeComponent } from './api-analytics-native.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakePagedResult, fakePlanV4, PlanV4 } from '../../../../../entities/management-api-v2';
import { fakeAnalyticsHistogram } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram.fixture';
import { fakeGroupByResponse } from '../../../../../entities/management-api-v2/analytics/analyticsGroupBy.fixture';
import { fakeAnalyticsStatsResponse } from '../../../../../entities/management-api-v2/analytics/analyticsStats.fixture';

describe('ApiAnalyticsNativeComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiAnalyticsNativeComponent>;
  let componentHarness: ApiAnalyticsNativeHarness;
  let httpTestingController: HttpTestingController;

  const initComponent = async (queryParams = {}) => {
    TestBed.configureTestingModule({
      imports: [ApiAnalyticsNativeComponent, OwlNativeDateTimeModule, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
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
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiAnalyticsNativeComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsNativeHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('GIVEN an API with analytics enabled', () => {
    beforeEach(async () => {
      await initComponent();
      expectPlanList();
      flushAllRequests();
    });

    it('should display analytics widgets in empty state', async () => {
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeFalsy();
    });

    it('should refresh when filters are applied', async () => {
      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.clickRefresh();
    });
  });

  describe('Query parameters', () => {
    const plan1 = fakePlanV4({ id: '1', name: 'plan 1' });
    const plan2 = fakePlanV4({ id: '2', name: 'plan 2' });

    it('should use default time range when no query params provided', async () => {
      await initComponent();
      expectPlanList();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValue = await filtersBar.getSelectedPeriod();

      expect(selectedValue).toEqual('Last day');
      flushAllRequests();
    });

    it('should use custom time range from query params', async () => {
      await initComponent({ period: '1M' });
      expectPlanList();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedValue = await filtersBar.getSelectedPeriod();

      expect(selectedValue).toEqual('Last month');
      flushAllRequests();
    });

    it('should use and select plans from query params', async () => {
      await initComponent({ plans: '1' });

      expectPlanList([plan1, plan2]);

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const selectedPlans = await filtersBar.getSelectedPlans();
      const selectedApplications = await filtersBar.getSelectedApplications();

      expect(selectedPlans).toEqual(['1']);
      expect(selectedApplications).toEqual(['1']);
      flushAllRequests();
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

      flushAllRequests();
    });

    it('should make backend calls with custom period from query params', async () => {
      await initComponent({ period: 'custom', from: '1', to: '2', plans: '1,2', applications: '1,2' });

      // Verify that backend calls are made with 7d time range
      const requests = httpTestingController.match(req => req.url.includes('/analytics'));
      expect(requests.length).toBeGreaterThan(0);

      // Check that requests include the correct time range
      requests
        .filter(request => !request.cancelled)
        // Filter request not available for plans and application query params
        .filter(request => !request.request.url.includes('downstream-active-connections'))
        .forEach(req => {
          expect(req.request.url).toContain('from=1');
          expect(req.request.url).toContain('to=2');
          expect(req.request.url).toContain('interval=0');
          expect(req.request.url).toContain('terms=plan-id:1,plan-id:2,app-id:1,app-id:2');
        });

      flushAllRequests();
    });

    it('should make backend call for histogram analytics type', async () => {
      await initComponent({ period: '1d' });

      const requests = httpTestingController.match(req => req.url.includes('/analytics'));

      const histogramRequests = requests.filter(req => req.request.url.includes('type=HISTOGRAM'));

      expect(histogramRequests.length).toBe(8);

      histogramRequests.forEach(req => {
        expect(req.request.url).toContain('type=HISTOGRAM');
        expect(req.request.url).toContain('from=');
        expect(req.request.url).toContain('to=');
        expect(req.request.url).toContain('interval=2880000');
        expect(req.request.url).toContain('aggregations=');
      });

      const trendRequests = requests.filter(req => req.request.url.includes('TREND:'));
      expect(trendRequests.length).toBe(1);

      const trendRateRequests = requests.filter(req => req.request.url.includes('TREND_RATE:'));
      expect(trendRateRequests.length).toBe(4);

      const valueRequests = requests.filter(req => req.request.url.includes('VALUE:'));
      expect(valueRequests.length).toBe(1);

      const deltaRequests = requests.filter(req => req.request.url.includes('DELTA:'));
      expect(deltaRequests.length).toBe(2);

      flushAllRequests();
    });

    it('should include API ID in all backend calls', async () => {
      await initComponent({ period: '1d' });

      const requests = httpTestingController.match(req => req.url.includes('/analytics'));
      expect(requests.length).toBeGreaterThan(0);

      // Verify all requests include the API ID
      requests.forEach(req => {
        expect(req.request.url).toContain(`/apis/${API_ID}/analytics`);
      });

      flushAllRequests();
    });

    it('should handle multiple concurrent requests with same parameters', async () => {
      await initComponent({ period: '1d' });

      // Should make multiple requests for different widgets
      const requests = httpTestingController.match(req => req.url.includes('/analytics'));
      expect(requests.length).toBe(8);

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

      flushAllRequests();
    });
  });

  function flushAllRequests() {
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
