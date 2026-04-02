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
import { AnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import { fakeAnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges.fixture';
import { AnalyticsResponseStatusOvertime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusOvertime';
import { AnalyticsResponseTimeOverTime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseTimeOverTime';
import { fakeAnalyticsResponseStatusOvertime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusOvertime.fixture';
import { fakeAnalyticsResponseTimeOverTime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseTimeOverTime.fixture';
import {
  fakeAnalyticsCount,
  fakeAnalyticsGroupBy,
  fakeAnalyticsStats,
} from '../../../../../entities/management-api-v2/analytics/analyticsUnified.fixture';

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
      expectAnalyticsStatusPie(fakeAnalyticsGroupBy({ values: { '200': 10 } }));
    });

    it('should display Traffic Overview - 4 stats cards', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('Traffic Overview');

      // All 4 cards start in loading state
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '', isLoading: true },
        { label: 'Avg Gateway Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      // Flush COUNT → first card loads
      expectAnalyticsCount(fakeAnalyticsCount({ count: 42 }));
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '42', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      // Flush STATS/gateway-rt → second card loads
      expectAnalyticsGatewayRt(fakeAnalyticsStats({ avg: 123.456 }));
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '42', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '123.456ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      // Flush STATS/upstream-rt → third card loads
      expectAnalyticsUpstreamRt(fakeAnalyticsStats({ avg: 88 }));
      // Flush STATS/content-length → fourth card loads
      expectAnalyticsContentLength(fakeAnalyticsStats({ avg: 512 }));
      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '42', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '123.456ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '88ms', isLoading: false },
        { label: 'Avg Content Length', value: '512bytes', isLoading: false },
      ]);

      // Flush remaining analytics request
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());
    });

    it('should display Response Status', async () => {
      const responseStatusRanges = await componentHarness.getResponseStatusRangesHarness('Response Status');

      // Expect loading
      expect(await responseStatusRanges.hasResponseStatusWithValues()).toBeFalsy();

      // Expect data
      expectApiAnalyticsResponseStatusRangesGetRequest(
        fakeAnalyticsResponseStatusRanges({
          ranges: {
            '300.0-400.0': 0,
            '100.0-200.0': 1,
            '200.0-300.0': 60,
            '400.0-500.0': 0,
            '500.0-600.0': 1,
          },
          rangesByEntrypoint: {
            'http-proxy': {
              '300.0-400.0': 0,
              '100.0-200.0': 1,
              '200.0-300.0': 60,
              '400.0-500.0': 0,
              '500.0-600.0': 1,
            },
          },
        }),
      );

      expect(await responseStatusRanges.hasResponseStatusWithValues()).toBeTruthy();

      // Flush remaining analytics requests
      expectAnalyticsCount(fakeAnalyticsCount());
      expectAnalyticsGatewayRt(fakeAnalyticsStats());
      expectAnalyticsUpstreamRt(fakeAnalyticsStats());
      expectAnalyticsContentLength(fakeAnalyticsStats());
    });

    it('should refresh', async () => {
      const requestStats = await componentHarness.getRequestStatsHarness('Traffic Overview');
      expectAnalyticsCount(fakeAnalyticsCount({ count: 10 }));
      expectAnalyticsGatewayRt(fakeAnalyticsStats({ avg: 50 }));
      expectAnalyticsUpstreamRt(fakeAnalyticsStats({ avg: 30 }));
      expectAnalyticsContentLength(fakeAnalyticsStats({ avg: 256 }));
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());

      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '10', isLoading: false },
        { label: 'Avg Gateway Response Time', value: '50ms', isLoading: false },
        { label: 'Avg Upstream Response Time', value: '30ms', isLoading: false },
        { label: 'Avg Content Length', value: '256bytes', isLoading: false },
      ]);

      const filtersBar = await componentHarness.getFiltersBarHarness();

      await filtersBar.refresh();

      expect(await requestStats.getValues()).toEqual([
        { label: 'Total Requests', value: '', isLoading: true },
        { label: 'Avg Gateway Response Time', value: '', isLoading: true },
        { label: 'Avg Upstream Response Time', value: '', isLoading: true },
        { label: 'Avg Content Length', value: '', isLoading: true },
      ]);

      expectAnalyticsCount(fakeAnalyticsCount());
      expectAnalyticsGatewayRt(fakeAnalyticsStats());
      expectAnalyticsUpstreamRt(fakeAnalyticsStats());
      expectAnalyticsContentLength(fakeAnalyticsStats());
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());
      expectAnalyticsStatusPie(fakeAnalyticsGroupBy());
      expectApiGetResponseStatusOvertime();
      expectApiGetResponseTimeOverTime();
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
      expectAnalyticsStatusPie(fakeAnalyticsGroupBy());
      expectAnalyticsCount(fakeAnalyticsCount());
      expectAnalyticsGatewayRt(fakeAnalyticsStats());
      expectAnalyticsUpstreamRt(fakeAnalyticsStats());
      expectAnalyticsContentLength(fakeAnalyticsStats());
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());
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

  function expectAnalyticsStatusPie(data: ReturnType<typeof fakeAnalyticsGroupBy>) {
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const req = httpTestingController.expectOne(
      (r) => r.method === 'GET' && r.url.startsWith(baseUrl) && r.url.includes('type=GROUP_BY') && r.url.includes('field=status'),
    );
    req.flush(data);
    fixture.detectChanges();
  }

  function expectAnalyticsCount(data: ReturnType<typeof fakeAnalyticsCount>) {
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const req = httpTestingController.expectOne((r) => r.method === 'GET' && r.url.startsWith(baseUrl) && r.url.includes('type=COUNT'));
    req.flush(data);
    fixture.detectChanges();
  }

  function expectAnalyticsGatewayRt(data: ReturnType<typeof fakeAnalyticsStats>) {
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const req = httpTestingController.expectOne(
      (r) => r.method === 'GET' && r.url.startsWith(baseUrl) && r.url.includes('type=STATS') && r.url.includes('field=gateway-response-time-ms'),
    );
    req.flush(data);
    fixture.detectChanges();
  }

  function expectAnalyticsUpstreamRt(data: ReturnType<typeof fakeAnalyticsStats>) {
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const req = httpTestingController.expectOne(
      (r) => r.method === 'GET' && r.url.startsWith(baseUrl) && r.url.includes('type=STATS') && r.url.includes('field=endpoint-response-time-ms'),
    );
    req.flush(data);
    fixture.detectChanges();
  }

  function expectAnalyticsContentLength(data: ReturnType<typeof fakeAnalyticsStats>) {
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const req = httpTestingController.expectOne(
      (r) => r.method === 'GET' && r.url.startsWith(baseUrl) && r.url.includes('type=STATS') && r.url.includes('field=request-content-length'),
    );
    req.flush(data);
    fixture.detectChanges();
  }

  function expectApiAnalyticsResponseStatusRangesGetRequest(analyticsResponseStatusRanges: AnalyticsResponseStatusRanges) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/response-status-ranges`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(analyticsResponseStatusRanges);
    fixture.detectChanges();
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
