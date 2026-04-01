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
import { AnalyticsCount, AnalyticsGroupBy, AnalyticsStats } from '../../../../../entities/management-api-v2/analytics/analyticsUnified';
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
    httpTestingController.verify();
  });

  it('should display loading', async () => {
    await initComponent();
    expect(await componentHarness.isLoaderDisplayed()).toBeTruthy();

    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'GET',
    });
  });

  describe('GIVEN an API with analytics.enabled=false', () => {
    beforeEach(async () => {
      await initComponent();
      expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: false } }));
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
      expectApiGetResponseStatusOvertime();
      expectApiGetResponseTimeOverTime();
    });

    it('should display Request Stats with 4 cards', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');

      // All 4 cards loading initially
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '', isLoading: true },
        { label: 'Avg GW Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      // Flush all analytics
      expectAnalyticsCount({ count: 100 });
      expectAnalyticsStats('gateway-response-time-ms', { count: 1, min: 0, max: 10, avg: 5.5, sum: 5.5 });
      expectAnalyticsStats('endpoint-response-time-ms', { count: 1, min: 0, max: 8, avg: 3, sum: 3 });
      expectAnalyticsStats('request-content-length', { count: 1, min: 0, max: 512, avg: 256, sum: 256 });
      expectAnalyticsGroupBy('status');

      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '100', isLoading: false },
        { label: 'Avg GW Response Time', value: '5.5ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '3ms', isLoading: false },
        { label: 'Avg Content Length', value: '256B', isLoading: false },
      ]);
    });

    it('should display Response Status from GROUP_BY', async () => {
      const responseStatusRanges = await componentHarness.getResponseStatusRangesHarness('Response Status');

      // Expect loading
      expect(await responseStatusRanges.hasResponseStatusWithValues()).toBeFalsy();

      // Flush GROUP_BY with individual status codes — should be bucketed
      expectAnalyticsGroupBy('status', { values: { '200': 60, '404': 1, '500': 1 }, metadata: {} });

      expect(await responseStatusRanges.hasResponseStatusWithValues()).toBeTruthy();

      // Flush remaining analytics
      expectAnalyticsCount();
      expectAnalyticsStats('gateway-response-time-ms');
      expectAnalyticsStats('endpoint-response-time-ms');
      expectAnalyticsStats('request-content-length');
    });

    it('should refresh', async () => {
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');

      expectAnalyticsCount({ count: 100 });
      expectAnalyticsStats('gateway-response-time-ms', { count: 1, min: 0, max: 10, avg: 5.5, sum: 5.5 });
      expectAnalyticsStats('endpoint-response-time-ms', { count: 1, min: 0, max: 8, avg: 3, sum: 3 });
      expectAnalyticsStats('request-content-length', { count: 1, min: 0, max: 512, avg: 256, sum: 256 });
      expectAnalyticsGroupBy('status');

      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '100', isLoading: false },
        { label: 'Avg GW Response Time', value: '5.5ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '3ms', isLoading: false },
        { label: 'Avg Content Length', value: '256B', isLoading: false },
      ]);

      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.refresh();

      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '', isLoading: true },
        { label: 'Avg GW Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      expectAnalyticsCount();
      expectAnalyticsStats('gateway-response-time-ms');
      expectAnalyticsStats('endpoint-response-time-ms');
      expectAnalyticsStats('request-content-length');
      expectAnalyticsGroupBy('status');
      expectApiGetResponseStatusOvertime();
      expectApiGetResponseTimeOverTime();
    });

    it('should show stats cards even when one stat call fails', async () => {
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');

      // Flush count and 2 stats successfully, error on endpoint-response-time-ms
      expectAnalyticsCount({ count: 50 });
      expectAnalyticsStats('gateway-response-time-ms', { count: 1, min: 0, max: 10, avg: 7, sum: 7 });

      const analyticsUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
      httpTestingController
        .expectOne(
          (r) => r.method === 'GET' && r.url.startsWith(analyticsUrl) && r.params.get('type') === 'STATS' && r.params.get('field') === 'endpoint-response-time-ms',
        )
        .error(new ProgressEvent('error'));

      expectAnalyticsStats('request-content-length', { count: 1, min: 0, max: 512, avg: 256, sum: 256 });
      expectAnalyticsGroupBy('status');

      // 3 cards show values, 1 (upstream) shows dash
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '50', isLoading: false },
        { label: 'Avg GW Response Time', value: '7ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '-', isLoading: false },
        { label: 'Avg Content Length', value: '256B', isLoading: false },
      ]);
    });
  });

  describe('Query parameters for enabled analytics', () => {
    [
      { input: {}, expected: 'Last day' },
      { input: { period: '1M' }, expected: 'Last month' },
      { input: { period: 'incorrect' }, expected: 'Last day' },
      { input: { otherParameter: 'otherParameter' }, expected: 'Last day' },
    ].forEach((testParams) => {
      it(`should display "${testParams.expected}" time range if query parameter is ${JSON.stringify(testParams.input)}`, async () => {
        await initComponent(testParams.input);
        expectAllAnalyticsCall();

        const filtersBar = await componentHarness.getFiltersBarHarness();

        const matSelect = await filtersBar.getMatSelect();
        const selectedValue = await matSelect.getValueText();

        expect(selectedValue).toEqual(testParams.expected);
      });
    });

    function expectAllAnalyticsCall() {
      expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: true } }));
      expectApiGetResponseStatusOvertime();
      expectApiGetResponseTimeOverTime();
      expectAnalyticsCount();
      expectAnalyticsStats('gateway-response-time-ms');
      expectAnalyticsStats('endpoint-response-time-ms');
      expectAnalyticsStats('request-content-length');
      expectAnalyticsGroupBy('status');
    }
  });

  function expectApiGetRequest(api: ApiV4) {
    const res = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
      method: 'GET',
    });

    res.flush(api);

    fixture.detectChanges();
  }

  function expectAnalyticsCount(res: AnalyticsCount = { count: 0 }) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(url) && r.params.get('type') === 'COUNT');
    req.flush(res);
  }

  function expectAnalyticsStats(field: string, res: AnalyticsStats = { count: 0, min: 0, max: 0, avg: 0, sum: 0 }) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const req = httpTestingController.expectOne(
      (r) => r.method === 'GET' && r.url.startsWith(url) && r.params.get('type') === 'STATS' && r.params.get('field') === field,
    );
    req.flush(res);
  }

  function expectAnalyticsGroupBy(field: string, res: AnalyticsGroupBy = { values: {}, metadata: {} }) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const req = httpTestingController.expectOne(
      (r) => r.method === 'GET' && r.url.startsWith(url) && r.params.get('type') === 'GROUP_BY' && r.params.get('field') === field,
    );
    req.flush(res);
  }

  function expectApiGetResponseStatusOvertime(res: AnalyticsResponseStatusOvertime = fakeAnalyticsResponseStatusOvertime()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/response-status-overtime`;
    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(url));
    req.flush(res);
  }

  function expectApiGetResponseTimeOverTime(res: AnalyticsResponseTimeOverTime = fakeAnalyticsResponseTimeOverTime()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/response-time-over-time`;
    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(url));
    req.flush(res);
  }
});
