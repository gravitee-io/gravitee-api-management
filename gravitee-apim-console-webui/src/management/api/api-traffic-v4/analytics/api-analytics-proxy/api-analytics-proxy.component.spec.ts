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
import { fakeAnalyticsHistogram } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram.fixture';
import {
  AggregationFields,
  AggregationTypes,
  HistogramAnalyticsResponse,
} from '../../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { fakeGroupByAnalyticsResponse } from '../../../../../entities/management-api-v2/analytics/analyticsGroupBy.fixture';
import { GroupByAnalyticsResponse } from '../../../../../entities/management-api-v2/analytics/analyticsGroupBy';

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

  describe('GIVEN an API with analytics.enabled=true', () => {
    beforeEach(async () => {
      await initComponent();
      expectGetHistogramAnalytics(fakeAnalyticsHistogram(), 2);
      expectGetGroupByAnalytics();
    });

    it('should serialize SINGLE aggregation into request params', async () => {
      fixture.componentInstance.chartWidgetConfigs = [
        {
          apiId: API_ID,
          aggregations: [
            {
              type: AggregationTypes.AVG,
              field: AggregationFields.GATEWAY_RESPONSE_TIME_MS,
            },
          ],
          title: 'Response Time Over Time',
          tooltip: 'Measures latency trend for gateway and downstream systems (API) ',
        },
      ];
      const expectedSerialization = 'AVG:gateway-response-time-ms';

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const select = await filtersBar.getMatSelect();
      await select.clickOptions({ text: 'Custom' });

      const from = '2025-07-10 15:21:00';
      const to = '2025-07-17 15:21:00';
      const fromInMilliSeconds = new Date(from).getTime();
      const toInMilliseconds = new Date(to).getTime();

      await filtersBar.setFromDate(from);
      await filtersBar.setToDate(to);

      await filtersBar.apply();

      const url2 = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics?type=HISTOGRAM&from=${fromInMilliSeconds}&to=${toInMilliseconds}&interval=20160000&aggregations=${expectedSerialization}`;
      const req2 = httpTestingController.expectOne(url2);
      req2.flush(fakeAnalyticsHistogram());
    });

    it('should serialize MULTIPLE aggregation into request params params', async () => {
      fixture.componentInstance.chartWidgetConfigs = [
        {
          apiId: API_ID,
          aggregations: [
            {
              type: AggregationTypes.AVG,
              field: AggregationFields.GATEWAY_RESPONSE_TIME_MS,
            },
            {
              type: AggregationTypes.AVG,
              field: AggregationFields.ENDPOINT_RESPONSE_TIME_MS,
            },
            {
              type: AggregationTypes.MAX,
              field: AggregationFields.ENDPOINT_RESPONSE_TIME_MS,
            },
          ],
          title: 'Response Time Over Time',
          tooltip: 'Measures latency trend for gateway and downstream systems (API) ',
        },
      ];

      const expectedMultipleSerialization = 'AVG:gateway-response-time-ms,AVG:endpoint-response-time-ms,MAX:endpoint-response-time-ms';

      const filters = await componentHarness.getFiltersBarHarness();
      const select = await filters.getMatSelect();
      await select.clickOptions({ text: 'Custom' });

      const from = '2025-07-10 15:21:00';
      const to = '2025-07-17 15:21:00';
      const fromInMilliSeconds = new Date(from).getTime();
      const toInMilliseconds = new Date(to).getTime();

      await filters.setFromDate(from);
      await filters.setToDate(to);

      await filters.apply();

      const url2 = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics?type=HISTOGRAM&from=${fromInMilliSeconds}&to=${toInMilliseconds}&interval=20160000&aggregations=${expectedMultipleSerialization}`;
      const req2 = httpTestingController.expectOne(url2);
      req2.flush(fakeAnalyticsHistogram());
    });

    it('should not display empty panel', async () => {
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeFalsy();
    });

    it('should refresh', async () => {
      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.refresh();
      expectGetHistogramAnalytics(fakeAnalyticsHistogram(), 2);
      expectGetGroupByAnalytics();
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
        expectGetHistogramAnalytics(fakeAnalyticsHistogram(), 2);
        expectGetGroupByAnalytics();

        const filtersBar = await componentHarness.getFiltersBarHarness();

        const matSelect = await filtersBar.getMatSelect();
        const selectedValue = await matSelect.getValueText();

        expect(selectedValue).toEqual(testParams.expected);
      });
    });
  });

  function expectGetHistogramAnalytics(res: HistogramAnalyticsResponse = fakeAnalyticsHistogram(), numberOfRequests: number = 1) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics?type=HISTOGRAM`;
    const requests = httpTestingController.match((req) => {
      return req.url.startsWith(url);
    });

    expect(requests.length).toBe(numberOfRequests);
    requests.forEach((request) => {
      request.flush(res);
    });
  }

  function expectGetGroupByAnalytics(res: GroupByAnalyticsResponse = fakeGroupByAnalyticsResponse(), numberOfRequests: number = 1) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics?type=GROUP_BY&field=status`;
    const requests = httpTestingController.match((req) => {
      return req.url.startsWith(url);
    });

    expect(requests.length).toBe(numberOfRequests);
    requests.forEach((request) => {
      request.flush(res);
    });
  }
});
