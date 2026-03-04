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
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { ApiAnalyticsProxyComponent } from './api-analytics-proxy.component';
import { ApiAnalyticsProxyHarness } from './api-analytics-proxy.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { AnalyticsResponseStatusOvertime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusOvertime';
import { AnalyticsResponseTimeOverTime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseTimeOverTime';
import { fakeAnalyticsResponseStatusOvertime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusOvertime.fixture';
import { fakeAnalyticsResponseTimeOverTime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseTimeOverTime.fixture';

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
          },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiAnalyticsProxyComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsProxyHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    // Flush any unflushed analytics requests (e.g. from refresh test where chart re-fetches can be async)
    const remaining = httpTestingController.match((r) => {
      const u = r.urlWithParams ?? r.url ?? '';
      return u.includes('response-time-over-time') || u.includes('response-status-overtime');
    });
    remaining.forEach((tr) => {
      try {
        const u = tr.request.urlWithParams ?? tr.request.url ?? '';
        if (u.includes('response-status-overtime')) tr.flush(fakeAnalyticsResponseStatusOvertime());
        else if (u.includes('response-time-over-time')) tr.flush(fakeAnalyticsResponseTimeOverTime());
      } catch {
        // Request may already be cancelled (e.g. component destroyed)
      }
    });
    httpTestingController.verify();
  });

  it('should display loading', async () => {
    await initComponent();
    expect(await componentHarness.isLoaderDisplayed()).toBeTruthy();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'GET',
    });
    req.flush(fakeApiV4({ id: API_ID }));
  });

  describe('GIVEN an API with analytics.enabled=false', () => {
    beforeEach(async () => {
      await initComponent();
      expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: false } }));
      fixture.detectChanges();
    });

    it('should display empty panel', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeTruthy();
    });
  });

  describe('GIVEN an API with analytics.enabled=true', () => {
    beforeEach(async () => {
      await initComponent();
      expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: true } }));
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should display Request Stats from unified analytics (COUNT + STATS)', async () => {
      flushV4AnalyticsRequests({
        count: 100,
        statsGateway: { avg: 12.5 },
        statsEndpoint: { avg: 8.3 },
        statsContentLength: { avg: 256 },
        groupByStatus: { values: { '200': 80, '404': 10 }, metadata: {} },
      });
      await new Promise((r) => setTimeout(r, 0)); // let component delay(0) emit so VM has data
      fixture.detectChanges(); // mount line charts so they send their requests
      expectApiGetResponseStatusOvertime();
      expectApiGetResponseTimeOverTime();
      fixture.detectChanges();

      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '100', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '12.5ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '8.3ms', isLoading: false },
        { label: 'Avg Content Length', value: '256bytes', isLoading: false },
      ]);
    });

    it('should display empty state when no analytics data for period (count 0)', async () => {
      flushV4AnalyticsRequests({
        count: 0,
        statsGateway: { avg: 0 },
        statsEndpoint: { avg: 0 },
        statsContentLength: { avg: 0 },
        groupByStatus: { values: {}, metadata: {} },
      });
      await new Promise((r) => setTimeout(r, 0));
      fixture.detectChanges();
      // No line charts when hasNoData (empty state shown instead of grid)
      fixture.detectChanges();

      const emptyStates = fixture.nativeElement.querySelectorAll('gio-card-empty-state');
      const noDataTitle = Array.from(emptyStates).find((el: Element) => el.textContent?.includes('No analytics data for this period'));
      expect(noDataTitle).toBeTruthy();
    });

    it('should display HTTP Status pie from GROUP_BY', async () => {
      flushV4AnalyticsRequests({
        count: 90,
        statsGateway: { avg: 0 },
        statsEndpoint: { avg: 0 },
        statsContentLength: { avg: 0 },
        groupByStatus: { values: { '200': 70, '404': 15, '500': 5 }, metadata: {} },
      });
      await new Promise((r) => setTimeout(r, 0));
      fixture.detectChanges();
      expectApiGetResponseStatusOvertime();
      expectApiGetResponseTimeOverTime();
      fixture.detectChanges();

      const responseStatusRanges = await componentHarness.getResponseStatusRangesHarness('HTTP Status');
      expect(await responseStatusRanges.hasResponseStatusWithValues()).toBeTruthy();
    });

    it('should refresh and re-fetch unified analytics', async () => {
      flushV4AnalyticsRequests({
        count: 42,
        statsGateway: { avg: 10 },
        statsEndpoint: { avg: 5 },
        statsContentLength: { avg: 100 },
        groupByStatus: { values: { '200': 42 }, metadata: {} },
      });
      await new Promise((r) => setTimeout(r, 0));
      fixture.detectChanges();
      flushAllLineChartRequests();
      fixture.detectChanges();

      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');
      expect(await requestStats.getValues()).toContainEqual({
        label: 'Total Requests',
        value: '42',
        isLoading: false,
      });

      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.refresh();

      flushV4AnalyticsRequests({
        count: 0,
        statsGateway: { avg: 0 },
        statsEndpoint: { avg: 0 },
        statsContentLength: { avg: 0 },
        groupByStatus: { values: {}, metadata: {} },
      });
      await new Promise((r) => setTimeout(r, 0));
      fixture.detectChanges();
      flushAllLineChartRequests();
      fixture.detectChanges();

      // When count is 0 we show empty state "No analytics data for this period" (AC 10)
      const emptyStates = fixture.nativeElement.querySelectorAll('gio-card-empty-state');
      const noDataTitle = Array.from(emptyStates).find((el: Element) => el.textContent?.includes('No analytics data for this period'));
      expect(noDataTitle).toBeTruthy();
    });
  });

  describe('GIVEN analytics enabled and one request fails', () => {
    it('should display error state (Analytics unavailable)', async () => {
      await initComponent();
      expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: true } }));
      await fixture.whenStable();
      fixture.detectChanges();

      const analyticsUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
      const countReq = httpTestingController.expectOne((r) => r.url.startsWith(analyticsUrl) && r.url.includes('type=COUNT'));
      countReq.flush('error', { status: 500, statusText: 'Internal Server Error' });

      flushV4AnalyticsRequests(
        { count: 0, statsGateway: { avg: 0 }, statsEndpoint: { avg: 0 }, statsContentLength: { avg: 0 }, groupByStatus: { values: {}, metadata: {} } },
        { skipCount: true },
      );
      // No line-chart requests: error view is shown so grid and line charts are never mounted
      await new Promise((r) => setTimeout(r, 0)); // let component delay(0) emit error state
      fixture.detectChanges();

      const emptyStates = fixture.nativeElement.querySelectorAll('gio-card-empty-state');
      const errorTitle = Array.from(emptyStates).find((el: Element) => el.textContent?.includes('Analytics unavailable'));
      expect(errorTitle).toBeTruthy();
    });
  });

  describe('Query parameters for enabled analytics', () => {
    [
      { input: {}, expected: 'Last 24 hours' },
      { input: { period: '1M' }, expected: 'Last 30 days' },
      { input: { period: 'incorrect' }, expected: 'Last 24 hours' },
      { input: { otherParameter: 'otherParameter' }, expected: 'Last 24 hours' },
    ].forEach((testParams) => {
      it(`should display "${testParams.expected}" time range if query parameter is ${JSON.stringify(testParams.input)}`, async () => {
        await initComponent(testParams.input);
        expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: true } }));
        await fixture.whenStable();
        fixture.detectChanges();
        flushV4AnalyticsRequests({
          count: 1,
          statsGateway: { avg: 0 },
          statsEndpoint: { avg: 0 },
          statsContentLength: { avg: 0 },
          groupByStatus: { values: { '200': 1 }, metadata: {} },
        });
        await new Promise((r) => setTimeout(r, 0));
        fixture.detectChanges();
        expectApiGetResponseStatusOvertime();
        expectApiGetResponseTimeOverTime();
        fixture.detectChanges();

        const filtersBar = await componentHarness.getFiltersBarHarness();
        const matSelect = await filtersBar.getMatSelect();
        const selectedValue = await matSelect.getValueText();
        expect(selectedValue).toEqual(testParams.expected);
      });
    });
  });

  function expectApiGetRequest(api: ApiV4) {
    const res = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
      method: 'GET',
    });
    res.flush(api);
  }

  function flushV4AnalyticsRequests(
    data: {
      count: number;
      statsGateway: { avg: number };
      statsEndpoint: { avg: number };
      statsContentLength: { avg: number };
      groupByStatus: { values: Record<string, number>; metadata: Record<string, unknown> };
    },
    options?: { skipCount?: boolean },
  ) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const match = (type: string, field?: string) => (req: { url: string }) =>
      req.url.startsWith(url) && req.url.includes(`type=${type}`) && (field == null || req.url.includes(`field=${encodeURIComponent(field)}`));

    if (!options?.skipCount) {
      const countReq = httpTestingController.expectOne((r) => match('COUNT')(r));
      countReq.flush({ type: 'COUNT', count: data.count });
    }
    const statsGatewayReq = httpTestingController.expectOne((r) => match('STATS', 'gateway-response-time-ms')(r));
    statsGatewayReq.flush({
      type: 'STATS',
      count: 1,
      min: data.statsGateway.avg,
      max: data.statsGateway.avg,
      avg: data.statsGateway.avg,
      sum: data.statsGateway.avg,
    });
    const statsEndpointReq = httpTestingController.expectOne((r) => match('STATS', 'endpoint-response-time-ms')(r));
    statsEndpointReq.flush({
      type: 'STATS',
      count: 1,
      min: data.statsEndpoint.avg,
      max: data.statsEndpoint.avg,
      avg: data.statsEndpoint.avg,
      sum: data.statsEndpoint.avg,
    });
    const statsContentLengthReq = httpTestingController.expectOne((r) => match('STATS', 'request-content-length')(r));
    statsContentLengthReq.flush({
      type: 'STATS',
      count: 1,
      min: data.statsContentLength.avg,
      max: data.statsContentLength.avg,
      avg: data.statsContentLength.avg,
      sum: data.statsContentLength.avg,
    });
    const groupByReq = httpTestingController.expectOne((r) => match('GROUP_BY', 'status')(r));
    groupByReq.flush({ type: 'GROUP_BY', values: data.groupByStatus.values, metadata: data.groupByStatus.metadata });
  }

  function expectApiGetResponseStatusOvertime(res: AnalyticsResponseStatusOvertime = fakeAnalyticsResponseStatusOvertime()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/response-status-overtime`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(res);
  }

  function expectApiGetResponseTimeOverTime(res: AnalyticsResponseTimeOverTime = fakeAnalyticsResponseTimeOverTime()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/response-time-over-time`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(res);
  }

  /** Flush all matching line-chart requests (e.g. after refresh when both charts re-fetch). */
  function flushAllLineChartRequests() {
    const statusReqs = httpTestingController.match((r) => r.method === 'GET' && (r.urlWithParams ?? r.url).includes('response-status-overtime'));
    statusReqs.forEach((req) => {
      try {
        req.flush(fakeAnalyticsResponseStatusOvertime());
      } catch {
        // Request may be cancelled (e.g. after refresh re-subscribe)
      }
    });
    const timeReqs = httpTestingController.match((r) => r.method === 'GET' && (r.urlWithParams ?? r.url).includes('response-time-over-time'));
    timeReqs.forEach((req) => {
      try {
        req.flush(fakeAnalyticsResponseTimeOverTime());
      } catch {
        // Request may be cancelled
      }
    });
  }
});
