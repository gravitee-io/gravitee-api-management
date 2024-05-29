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
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ApiAnalyticsMessageComponent } from './api-analytics-message.component';
import { ApiAnalyticsMessageHarness } from './api-analytics-message.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ApiV4, ConnectorPlugin, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { fakeAnalyticsRequestsCount } from '../../../../../entities/management-api-v2/analytics/analyticsRequestsCount.fixture';
import { AnalyticsRequestsCount } from '../../../../../entities/management-api-v2/analytics/analyticsRequestsCount';
import { AnalyticsAverageConnectionDuration } from '../../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { fakeAnalyticsAverageConnectionDuration } from '../../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration.fixture';

const ENTRYPOINTS: Partial<ConnectorPlugin>[] = [
  { id: 'http-get', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP', name: 'HTTP GET', deployed: true },
];

describe('ApiAnalyticsMessageComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiAnalyticsMessageComponent>;
  let componentHarness: ApiAnalyticsMessageHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsMessageComponent, NoopAnimationsModule, HttpClientTestingModule, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
            },
          },
        },
      ],
    }).compileComponents();

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiAnalyticsMessageComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsMessageHarness);
    fixture.autoDetectChanges(true);
  });

  it('should display loading', async () => {
    expect(await componentHarness.isLoaderDisplayed()).toBeTruthy();
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
      const requestStats = await componentHarness.getRequestStatsHarness('All Entrypoints - Request Stats');

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

      // Expect data
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
    });

    it('should display HTTP GET - Request Stats', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('HTTP GET Entrypoint - Request Stats');

      // Expect loading
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total requests',
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
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total requests',
          value: '42',
          isLoading: false,
        },
        {
          label: 'Average Connection Duration',
          value: '42.123ms',
          isLoading: false,
        },
      ]);
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

  function expectGetEntrypoints(): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`, method: 'GET' }).flush(ENTRYPOINTS);
  }
});
