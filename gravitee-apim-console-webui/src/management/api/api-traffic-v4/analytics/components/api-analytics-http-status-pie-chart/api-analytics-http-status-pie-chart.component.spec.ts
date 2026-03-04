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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { ApiAnalyticsHttpStatusPieChartComponent } from './api-analytics-http-status-pie-chart.component';
import { ApiAnalyticsHttpStatusPieChartHarness } from './api-analytics-http-status-pie-chart.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { fakeAnalyticsGroupBy } from '../../../../../../entities/management-api-v2/analytics/analyticsGroupBy.fixture';
import { timeFrameRangesParams } from '../../../../../../shared/utils/timeFrameRanges';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';

describe('ApiAnalyticsHttpStatusPieChartComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiAnalyticsHttpStatusPieChartComponent>;
  let harness: ApiAnalyticsHttpStatusPieChartHarness;
  let httpTestingController: HttpTestingController;
  let apiAnalyticsService: ApiAnalyticsV2Service;

  const expectGroupByRequest = (response: { type: 'GROUP_BY'; values: Record<string, number>; metadata: Record<string, Record<string, string>> }) => {
    const req = httpTestingController.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`) &&
        r.params.get('type') === 'GROUP_BY' &&
        r.params.get('field') === 'status',
    );
    req.flush(response);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsHttpStatusPieChartComponent, NoopAnimationsModule, GioTestingModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    apiAnalyticsService = TestBed.inject(ApiAnalyticsV2Service);
    apiAnalyticsService.setTimeRangeFilter(timeFrameRangesParams('1d'));

    fixture = TestBed.createComponent(ApiAnalyticsHttpStatusPieChartComponent);
    fixture.componentRef.setInput('apiId', API_ID);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsHttpStatusPieChartHarness);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController?.verify();
  });

  it('should display loading initially', async () => {
    expect(await harness.isLoaderDisplayed()).toBeTruthy();
    expectGroupByRequest(fakeAnalyticsGroupBy());
  });

  it('should map GROUP_BY values to pie chart and display chart', async () => {
    expectGroupByRequest(
      fakeAnalyticsGroupBy({
        values: { '200': 80, '404': 15, '500': 5 },
        metadata: { '200': { name: 'OK' }, '404': { name: 'Not Found' }, '500': { name: 'Server Error' } },
      }),
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await harness.isLoaderDisplayed()).toBeFalsy();
    expect(await harness.isEmptyStateDisplayed()).toBeFalsy();
    expect(await harness.isChartDisplayed()).toBeTruthy();
  });

  it('should show empty state when GROUP_BY returns empty values', async () => {
    expectGroupByRequest(fakeAnalyticsGroupBy({ values: {}, metadata: {} }));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await harness.isLoaderDisplayed()).toBeFalsy();
    expect(await harness.isEmptyStateDisplayed()).toBeTruthy();
    expect(await harness.isChartDisplayed()).toBeFalsy();
  });

  it('should show empty state on error', async () => {
    const req = httpTestingController.expectOne(
      (r) =>
        r.method === 'GET' &&
        r.url.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`) &&
        r.params.get('type') === 'GROUP_BY',
    );
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await harness.isLoaderDisplayed()).toBeFalsy();
    expect(await harness.isEmptyStateDisplayed()).toBeTruthy();
    expect(await harness.isChartDisplayed()).toBeFalsy();
  });

  it('should filter out zero values from pie chart', async () => {
    expectGroupByRequest(
      fakeAnalyticsGroupBy({
        values: { '200': 10, '404': 0, '500': 5 },
        metadata: {},
      }),
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(await harness.isChartDisplayed()).toBeTruthy();
  });
});
