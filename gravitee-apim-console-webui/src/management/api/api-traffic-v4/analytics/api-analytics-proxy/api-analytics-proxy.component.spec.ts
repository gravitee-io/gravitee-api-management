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

import { GioTestingModule } from '../../../../../shared/testing';
import { fakeAnalyticsHistogram } from '../../../../../entities/management-api-v2/analytics/analyticsHistogram.fixture';
import { fakeGroupByResponse } from '../../../../../entities/management-api-v2/analytics/analyticsGroupBy.fixture';

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

  describe('GIVEN an API with analytics enabled', () => {
    beforeEach(async () => {
      await initComponent();
      // Handle all initial requests
      handleAllRequests();
    });

    it('should display analytics widgets in empty state', async () => {
      expect(await componentHarness.isEmptyPanelDisplayed()).toBeFalsy();
    });

    it('should refresh when filters are applied', async () => {
      const filtersBar = await componentHarness.getFiltersBarHarness();
      await filtersBar.refresh();

      // Handle all refresh requests
      handleAllRequests();
    });
  });

  describe('Query parameters', () => {
    it('should use default time range when no query params provided', async () => {
      await initComponent();
      handleAllRequests();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const matSelect = await filtersBar.getMatSelect();
      const selectedValue = await matSelect.getValueText();

      expect(selectedValue).toEqual('Last day');
    });

    it('should use custom time range from query params', async () => {
      await initComponent({ period: '1M' });
      handleAllRequests();

      const filtersBar = await componentHarness.getFiltersBarHarness();
      const matSelect = await filtersBar.getMatSelect();
      const selectedValue = await matSelect.getValueText();

      expect(selectedValue).toEqual('Last month');
    });
  });

  function handleAllRequests() {
    // Handle all pending requests
    const requests = httpTestingController.match(() => true);
    requests.forEach((request) => {
      if (request.request.url.includes('type=HISTOGRAM')) {
        request.flush(fakeAnalyticsHistogram());
      } else if (request.request.url.includes('type=GROUP_BY')) {
        request.flush(fakeGroupByResponse());
      }
    });
  }
});
