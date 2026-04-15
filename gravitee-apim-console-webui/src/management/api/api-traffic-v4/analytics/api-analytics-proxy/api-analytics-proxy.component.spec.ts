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
import { fakeAnalyticsRequestsCount } from '../../../../../entities/management-api-v2/analytics/analyticsRequestsCount.fixture';
import { AnalyticsRequestsCount } from '../../../../../entities/management-api-v2/analytics/analyticsRequestsCount';
import { AnalyticsAverageConnectionDuration } from '../../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { fakeAnalyticsAverageConnectionDuration } from '../../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration.fixture';
import { AnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import { fakeAnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges.fixture';
import { AnalyticsResponseStatusOvertime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusOvertime';
import { AnalyticsResponseTimeOverTime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseTimeOverTime';
import { fakeAnalyticsResponseStatusOvertime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusOvertime.fixture';
import { fakeAnalyticsResponseTimeOverTime } from '../../../../../entities/management-api-v2/analytics/analyticsResponseTimeOverTime.fixture';
import {
  fakeCountResponse,
  fakeGroupByResponse,
  fakeStatsResponse,
} from '../../../../../entities/management-api-v2/analytics/analyticsResponse.fixture';

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
      expectNewWidgetRequests({ occurrences: 2 });
    });

    it('should display HTTP Proxy Entrypoint - Request Stats', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');

      // Expect loading
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '',
          isLoading: true,
        },
        {
          label: 'Average Connection Duration',
          value: '',
          isLoading: true,
        },
      ]);

      // Expect incremental loading
      expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '0',
          isLoading: false,
        },
        {
          label: 'Average Connection Duration',
          value: '',
          isLoading: true,
        },
      ]);

      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration({ average: 42.1234556 }));
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '0',
          isLoading: false,
        },
        {
          label: 'Average Connection Duration',
          value: '42.123ms',
          isLoading: false,
        },
      ]);

      // Expect others analytics
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

      // Expect others analytics
      expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());
      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration());
    });

    it('should refresh', async () => {
      const requestStats = await componentHarness.getRequestStatsHarness('Request Stats');
      expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());
      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration({ average: 42.1234556 }));
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());

      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '0',
          isLoading: false,
        },
        {
          label: 'Average Connection Duration',
          value: '42.123ms',
          isLoading: false,
        },
      ]);

      const filtersBar = await componentHarness.getFiltersBarHarness();

      await filtersBar.refresh();

      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '',
          isLoading: true,
        },
        {
          label: 'Average Connection Duration',
          value: '',
          isLoading: true,
        },
      ]);

      expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());
      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration());
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());
      expectApiGetResponseStatusOvertime();
      expectApiGetResponseTimeOverTime();
      expectNewWidgetRequests({ occurrences: 2 });
    });
  });

  it('should display page-level empty-state banner when all new widgets are empty', async () => {
    await initComponent();
    expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: true } }));
    expectApiGetResponseStatusOvertime();
    expectApiGetResponseTimeOverTime();
    expectNewWidgetRequests({
      occurrences: 2,
      countResponse: fakeCountResponse({ count: 0 }),
      statsResponse: fakeStatsResponse({ count: 0, min: 0, max: 0, avg: 0, sum: 0 }),
      groupByResponse: fakeGroupByResponse({ values: {}, metadata: {} }),
    });
    expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());
    expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration());
    expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());
    fixture.detectChanges();

    expect(await componentHarness.isPageEmptyStateBannerDisplayed()).toBeTruthy();
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
      expectNewWidgetRequests({ occurrences: 2 });
      expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());
      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration());
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

  function expectApiAnalyticsRequestsCountGetRequest(analyticsRequestsCount: AnalyticsRequestsCount) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/requests-count`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(analyticsRequestsCount);
  }

  function expectApiAnalyticsAverageConnectionDurationGetRequest(analyticsAverageConnectionDuration: AnalyticsAverageConnectionDuration) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/average-connection-duration`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(analyticsAverageConnectionDuration);
  }

  function expectApiAnalyticsResponseStatusRangesGetRequest(analyticsResponseStatusRanges: AnalyticsResponseStatusRanges) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/response-status-ranges`;

    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(analyticsResponseStatusRanges);
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

  /**
   * Flushes the 5 HTTP requests fired by the new US-07 stats cards and US-08 status pie
   * widgets when the time range filter is set.
   */
  function expectNewWidgetRequests({
    occurrences = 1,
    countResponse = fakeCountResponse(),
    statsResponse = fakeStatsResponse(),
    groupByResponse = fakeGroupByResponse(),
  }: {
    occurrences?: number;
    countResponse?: ReturnType<typeof fakeCountResponse>;
    statsResponse?: ReturnType<typeof fakeStatsResponse>;
    groupByResponse?: ReturnType<typeof fakeGroupByResponse>;
  } = {}) {
    const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
    const countReqs = httpTestingController.match((r) => r.url === baseUrl && r.method === 'GET' && r.params.get('type') === 'COUNT');
    expect(countReqs.length).toBe(occurrences);
    countReqs.forEach((req) => req.flush(countResponse));

    const gatewayReqs = httpTestingController.match(
      (r) =>
        r.url === baseUrl && r.method === 'GET' && r.params.get('type') === 'STATS' && r.params.get('field') === 'gateway-response-time-ms',
    );
    expect(gatewayReqs.length).toBe(occurrences);
    gatewayReqs.forEach((req) => req.flush(statsResponse));

    const upstreamReqs = httpTestingController.match(
      (r) =>
        r.url === baseUrl &&
        r.method === 'GET' &&
        r.params.get('type') === 'STATS' &&
        r.params.get('field') === 'endpoint-response-time-ms',
    );
    expect(upstreamReqs.length).toBe(occurrences);
    upstreamReqs.forEach((req) => req.flush(statsResponse));

    const contentReqs = httpTestingController.match(
      (r) =>
        r.url === baseUrl && r.method === 'GET' && r.params.get('type') === 'STATS' && r.params.get('field') === 'request-content-length',
    );
    expect(contentReqs.length).toBe(occurrences);
    contentReqs.forEach((req) => req.flush(statsResponse));

    const groupByReqs = httpTestingController.match(
      (r) => r.url === baseUrl && r.method === 'GET' && r.params.get('type') === 'GROUP_BY' && r.params.get('field') === 'status',
    );
    expect(groupByReqs.length).toBe(occurrences);
    groupByReqs.forEach((req) => req.flush(groupByResponse));
  }
});
