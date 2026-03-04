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
import { fakeAnalyticsCount } from '../../../../../entities/management-api-v2/analytics/analyticsCount.fixture';
import { fakeAnalyticsStats } from '../../../../../entities/management-api-v2/analytics/analyticsStats.fixture';
import { fakeAnalyticsGroupBy } from '../../../../../entities/management-api-v2/analytics/analyticsGroupBy.fixture';
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

    it('should display HTTP Proxy Entrypoint - Request Stats', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');

      // Expect loading (4 cards)
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '', isLoading: true },
        { label: 'Avg Gateway Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      // Expect incremental loading
      expectUnifiedAnalyticsCount(fakeAnalyticsCount({ count: 100 }));
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '100', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      expectUnifiedAnalyticsStats('gateway-response-time-ms', fakeAnalyticsStats({ avg: 42.12 }));
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '100', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '42.12ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      expectUnifiedAnalyticsStats('endpoint-response-time-ms', fakeAnalyticsStats({ avg: 12.5 }));
      expectUnifiedAnalyticsStats('request-content-length', fakeAnalyticsStats({ avg: 256 }));
      expectUnifiedAnalyticsGroupBy(fakeAnalyticsGroupBy());
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '100', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '42.12ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '12.5ms', isLoading: false },
        { label: 'Avg Content Length', value: '256B', isLoading: false },
      ]);

      expect(await componentHarness.isPieChartPresent()).toBeTruthy();
      expect(await componentHarness.isResponseStatusOvertimePresent()).toBeTruthy();
      expect(await componentHarness.isResponseTimeOverTimePresent()).toBeTruthy();
    });

    it('should refresh', async () => {
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');
      expectUnifiedAnalyticsCount(fakeAnalyticsCount({ count: 100 }));
      expectUnifiedAnalyticsStats('gateway-response-time-ms', fakeAnalyticsStats({ avg: 42.12 }));
      expectUnifiedAnalyticsStats('endpoint-response-time-ms', fakeAnalyticsStats({ avg: 12 }));
      expectUnifiedAnalyticsStats('request-content-length', fakeAnalyticsStats({ avg: 256 }));
      expectUnifiedAnalyticsGroupBy(fakeAnalyticsGroupBy());

      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '100', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '42.12ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '12ms', isLoading: false },
        { label: 'Avg Content Length', value: '256B', isLoading: false },
      ]);

      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.refresh();

      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '', isLoading: true },
        { label: 'Avg Gateway Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      expectUnifiedAnalyticsCount(fakeAnalyticsCount());
      expectUnifiedAnalyticsStats('gateway-response-time-ms', fakeAnalyticsStats());
      expectUnifiedAnalyticsStats('endpoint-response-time-ms', fakeAnalyticsStats());
      expectUnifiedAnalyticsStats('request-content-length', fakeAnalyticsStats());
      expectUnifiedAnalyticsGroupBy(fakeAnalyticsGroupBy());
      expectApiGetResponseStatusOvertime();
      expectApiGetResponseTimeOverTime();
    });

    it('should degrade gracefully when unified endpoint returns 500', async () => {
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');
      const countReq = httpTestingController.expectOne(
        (r) => r.method === 'GET' && r.url.includes('/analytics') && r.params.get('type') === 'COUNT',
      );
      countReq.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
      fixture.detectChanges();
      await fixture.whenStable();

      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '-', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      expectUnifiedAnalyticsStats('gateway-response-time-ms', fakeAnalyticsStats());
      expectUnifiedAnalyticsStats('endpoint-response-time-ms', fakeAnalyticsStats());
      expectUnifiedAnalyticsStats('request-content-length', fakeAnalyticsStats());
      expectUnifiedAnalyticsGroupBy(fakeAnalyticsGroupBy());
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
      expectUnifiedAnalyticsCount(fakeAnalyticsCount());
      expectUnifiedAnalyticsStats('gateway-response-time-ms', fakeAnalyticsStats());
      expectUnifiedAnalyticsStats('endpoint-response-time-ms', fakeAnalyticsStats());
      expectUnifiedAnalyticsStats('request-content-length', fakeAnalyticsStats());
      expectUnifiedAnalyticsGroupBy(fakeAnalyticsGroupBy());
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

  function expectUnifiedAnalyticsCount(response: { type: 'COUNT'; count: number }) {
    const req = httpTestingController.expectOne(
      (r) => r.method === 'GET' && r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`) && r.params.get('type') === 'COUNT',
    );
    req.flush(response);
  }

  function expectUnifiedAnalyticsStats(field: string, response: { type: 'STATS'; count: number; min: number; max: number; avg: number; sum: number }) {
    const req = httpTestingController.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`) &&
        r.params.get('type') === 'STATS' &&
        r.params.get('field') === field,
    );
    req.flush(response);
  }

  function expectUnifiedAnalyticsGroupBy(response: { type: 'GROUP_BY'; values: Record<string, number>; metadata: Record<string, Record<string, string>> }) {
    const req = httpTestingController.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`) &&
        r.params.get('type') === 'GROUP_BY' &&
        r.params.get('field') === 'status',
    );
    req.flush(response);
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
});
