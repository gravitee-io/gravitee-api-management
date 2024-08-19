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

import { ApiAnalyticsMessageComponent } from './api-analytics-message.component';
import { ApiAnalyticsMessageHarness } from './api-analytics-message.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ApiV4, ConnectorPlugin, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { fakeAnalyticsRequestsCount } from '../../../../../entities/management-api-v2/analytics/analyticsRequestsCount.fixture';
import { AnalyticsRequestsCount } from '../../../../../entities/management-api-v2/analytics/analyticsRequestsCount';
import { AnalyticsAverageConnectionDuration } from '../../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { fakeAnalyticsAverageConnectionDuration } from '../../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration.fixture';
import { AnalyticsAverageMessagesPerRequest } from '../../../../../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest';
import { fakeAnalyticsAverageMessagesPerRequest } from '../../../../../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest.fixture';
import { AnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges';
import { fakeAnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges.fixture';

const ENTRYPOINTS: Partial<ConnectorPlugin>[] = [
  { id: 'http-get', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP', name: 'HTTP GET', deployed: true },
  { id: 'http-post', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP', name: 'HTTP POST', deployed: false },
];

describe('ApiAnalyticsMessageComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiAnalyticsMessageComponent>;
  let componentHarness: ApiAnalyticsMessageHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsMessageComponent, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
            },
          },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiAnalyticsMessageComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsMessageHarness);
    fixture.autoDetectChanges(true);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display loading', async () => {
    expect(await componentHarness.isLoaderDisplayed()).toBeTruthy();

    expectGetEntrypoints();
    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'GET',
    });
  });

  describe('GIVEN an API with analytics.enabled=false', () => {
    beforeEach(async () => {
      expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: false } }));
      expectGetEntrypoints();
    });

    it('should display empty panel', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeTruthy();
    });
  });

  describe('GIVEN an API with analytics.enabled=true', () => {
    beforeEach(async () => {
      expectApiGetRequest(
        fakeApiV4({
          id: API_ID,
          analytics: { enabled: true },
          listeners: [
            {
              type: 'HTTP',
              entrypoints: [
                {
                  type: 'http-get',
                },
              ],
            },
          ],
        }),
      );
      expectGetEntrypoints();
    });

    it('should display All Entrypoints - Request Stats', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('overview-request-stats');

      // Expect loading
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '',
          isLoading: true,
        },
        {
          label: 'Average Messages Per Request',
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
          label: 'Average Messages Per Request',
          value: '',
          isLoading: true,
        },
        {
          label: 'Average Connection Duration',
          value: '',
          isLoading: true,
        },
      ]);

      // Expect data
      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration({ average: 42.1234556 }));
      expectApiAnalyticsAverageMessagesPerRequestGetRequest(fakeAnalyticsAverageMessagesPerRequest());
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '0',
          isLoading: false,
        },
        {
          label: 'Average Messages Per Request',
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

    it('should display HTTP GET - Request Stats', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('http-get-request-stats');

      // Expect loading
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '',
          isLoading: true,
        },
        {
          label: 'Average Messages Per Request',
          value: '',
          isLoading: true,
        },
        {
          label: 'Average Connection Duration',
          value: '',
          isLoading: true,
        },
      ]);

      // Expect data
      expectApiAnalyticsAverageConnectionDurationGetRequest(
        fakeAnalyticsAverageConnectionDuration({ averagesByEntrypoint: { 'http-get': 42.1234556 } }),
      );
      expectApiAnalyticsRequestsCountGetRequest(
        fakeAnalyticsRequestsCount({
          countsByEntrypoint: { 'http-get': 42 },
        }),
      );
      expectApiAnalyticsAverageMessagesPerRequestGetRequest(fakeAnalyticsAverageMessagesPerRequest());
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '42',
          isLoading: false,
        },
        {
          label: 'Average Messages Per Request',
          value: '-',
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
      const overviewResponseStatusRanges = await componentHarness.getResponseStatusRangesHarness('overview-response-status-ranges');
      const httpGetResponseStatusRanges = await componentHarness.getResponseStatusRangesHarness('http-get-response-status-ranges');

      // Expect loading
      expect(await overviewResponseStatusRanges.hasResponseStatusWithValues()).toBeFalsy();
      expect(await httpGetResponseStatusRanges.hasResponseStatusWithValues()).toBeFalsy();

      // Expect data
      expectApiAnalyticsResponseStatusRangesGetRequest(
        fakeAnalyticsResponseStatusRanges({
          ranges: {
            '200': 0,
          },
          rangesByEntrypoint: {
            'http-get': {
              '200': 0,
            },
          },
        }),
      );
      expect(await overviewResponseStatusRanges.hasResponseStatusWithValues()).toBeTruthy();
      expect(await httpGetResponseStatusRanges.hasResponseStatusWithValues()).toBeTruthy();

      // Expect others analytics
      expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());
      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration({ average: 42.1234556 }));
      expectApiAnalyticsAverageMessagesPerRequestGetRequest(fakeAnalyticsAverageMessagesPerRequest());
    });

    it('should display HTTP POST (not configured) - Request Stats', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();

      expectApiAnalyticsRequestsCountGetRequest(
        fakeAnalyticsRequestsCount({
          countsByEntrypoint: { 'http-post': 42 },
        }),
      );
      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration());
      expectApiAnalyticsAverageMessagesPerRequestGetRequest(fakeAnalyticsAverageMessagesPerRequest());
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());
      const requestStats = await componentHarness.getRequestStatsHarness('http-post-request-stats');
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '42',
          isLoading: false,
        },
        {
          label: 'Average Messages Per Request',
          value: '-',
          isLoading: false,
        },
        {
          label: 'Average Connection Duration',
          value: '-',
          isLoading: false,
        },
      ]);

      expect(await componentHarness.getEntrypointRow()).toEqual([
        {
          name: 'Overview',
          isNotConfigured: false,
        },
        {
          name: 'HTTP GET',
          isNotConfigured: false,
        },
        {
          name: 'HTTP POST',
          isNotConfigured: true,
        },
      ]);
    });

    it('should refresh', async () => {
      const requestStats = await componentHarness.getRequestStatsHarness('overview-request-stats');
      expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());
      expectApiAnalyticsAverageConnectionDurationGetRequest(fakeAnalyticsAverageConnectionDuration({ average: 42.1234556 }));
      expectApiAnalyticsAverageMessagesPerRequestGetRequest(fakeAnalyticsAverageMessagesPerRequest());
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total Requests',
          value: '0',
          isLoading: false,
        },
        {
          label: 'Average Messages Per Request',
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
          label: 'Average Messages Per Request',
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
      expectApiAnalyticsAverageMessagesPerRequestGetRequest(fakeAnalyticsAverageMessagesPerRequest());
      expectApiAnalyticsResponseStatusRangesGetRequest(fakeAnalyticsResponseStatusRanges());
    });
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
    fixture.detectChanges();
  }

  function expectApiAnalyticsRequestsCountGetRequest(analyticsRequestsCount: AnalyticsRequestsCount) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/requests-count`,
        method: 'GET',
      })
      .flush(analyticsRequestsCount);
  }

  function expectApiAnalyticsAverageConnectionDurationGetRequest(analyticsAverageConnectionDuration: AnalyticsAverageConnectionDuration) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/average-connection-duration`,
        method: 'GET',
      })
      .flush(analyticsAverageConnectionDuration);
  }

  function expectApiAnalyticsAverageMessagesPerRequestGetRequest(analyticsAverageMessagesPerRequest: AnalyticsAverageMessagesPerRequest) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/average-messages-per-request`,
        method: 'GET',
      })
      .flush(analyticsAverageMessagesPerRequest);
  }

  function expectApiAnalyticsResponseStatusRangesGetRequest(analyticsResponseStatusRanges: AnalyticsResponseStatusRanges) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/response-status-ranges`,
        method: 'GET',
      })
      .flush(analyticsResponseStatusRanges);
  }

  function expectGetEntrypoints(): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`, method: 'GET' }).flush(ENTRYPOINTS);
  }
});
