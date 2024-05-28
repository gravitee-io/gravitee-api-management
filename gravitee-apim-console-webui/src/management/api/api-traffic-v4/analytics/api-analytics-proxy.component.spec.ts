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

import { ApiAnalyticsProxyComponent } from './api-analytics-proxy.component';
import { ApiAnalyticsProxyHarness } from './api-analytics-proxy.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../entities/management-api-v2';
import { fakeAnalyticsRequestsCount } from '../../../../entities/management-api-v2/analytics/analyticsRequestsCount.fixture';
import { AnalyticsRequestsCount } from '../../../../entities/management-api-v2/analytics/analyticsRequestsCount';

describe('ApiAnalyticsProxyComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiAnalyticsProxyComponent>;
  let componentHarness: ApiAnalyticsProxyHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsProxyComponent, NoopAnimationsModule, HttpClientTestingModule, MatIconTestingModule, GioTestingModule],
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
    fixture = TestBed.createComponent(ApiAnalyticsProxyComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsProxyHarness);
    fixture.autoDetectChanges(true);
  });

  it('should display loading', async () => {
    expect(await componentHarness.isLoaderDisplayed()).toBeTruthy();
  });

  describe('GIVEN an API with analytics.enabled=false', () => {
    beforeEach(async () => {
      expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: false } }));
    });

    it('should display empty panel', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeTruthy();
    });
  });

  describe('GIVEN an API with analytics.enabled=true', () => {
    beforeEach(async () => {
      expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: true } }));
    });
    it('should display HTTP Proxy Entrypoint - Request Stats', async () => {
      expect(await componentHarness.isLoaderDisplayed()).toBeFalsy();
      const requestStats = await componentHarness.getRequestStatsHarness('HTTP Proxy Entrypoint - Request Stats');

      // Expect loading
      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total requests',
          value: '',
          isLoading: true,
        },
      ]);

      expectApiAnalyticsRequestsCountGetRequest(fakeAnalyticsRequestsCount());

      expect(await requestStats.getValues()).toEqual([
        {
          label: 'Total requests',
          value: '0',
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
});
