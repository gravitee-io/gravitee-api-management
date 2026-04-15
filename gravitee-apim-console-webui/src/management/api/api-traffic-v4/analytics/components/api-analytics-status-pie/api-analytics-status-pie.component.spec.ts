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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ApiAnalyticsStatusPieComponent } from './api-analytics-status-pie.component';
import { ApiAnalyticsStatusPieHarness } from './api-analytics-status-pie.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { fakeGroupByResponse } from '../../../../../../entities/management-api-v2/analytics/analyticsResponse.fixture';

const API_ID = 'test-api-id';
const BASE_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
const FROM = 1_700_000_000_000;
const TO = 1_700_086_400_000;

describe('ApiAnalyticsStatusPieComponent', () => {
  let fixture: ComponentFixture<ApiAnalyticsStatusPieComponent>;
  let httpTestingController: HttpTestingController;
  let harness: ApiAnalyticsStatusPieHarness;
  let analyticsService: ApiAnalyticsV2Service;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsStatusPieComponent, GioTestingModule, NoopAnimationsModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    analyticsService = TestBed.inject(ApiAnalyticsV2Service);

    fixture = TestBed.createComponent(ApiAnalyticsStatusPieComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsStatusPieHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('loading state', () => {
    it('should show loader while the request is in flight', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(true);
      expect(await harness.isChartDisplayed()).toBe(false);

      flushGroupByRequest();
    });
  });

  describe('populated state', () => {
    it('should display the chart when GROUP_BY returns data', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushGroupByRequest(fakeGroupByResponse({ values: { '200': 80, '404': 15, '500': 5 } }));
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.isChartDisplayed()).toBe(true);
      expect(await harness.isEmptyStateDisplayed()).toBe(false);
      expect(await harness.isErrorDisplayed()).toBe(false);
    });

    it('should call GROUP_BY endpoint with correct params', () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();

      const req = httpTestingController.expectOne((r) => r.url === BASE_URL && r.method === 'GET');
      expect(req.request.params.get('type')).toBe('GROUP_BY');
      expect(req.request.params.get('field')).toBe('status');
      expect(req.request.params.get('size')).toBe('10');
      expect(+req.request.params.get('from')).toBe(FROM);
      expect(+req.request.params.get('to')).toBe(TO);
      req.flush(fakeGroupByResponse());
    });
  });

  describe('empty state', () => {
    it('should display empty state when response.values is empty', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushGroupByRequest(fakeGroupByResponse({ values: {}, metadata: {} }));
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.isChartDisplayed()).toBe(false);
      expect(await harness.isEmptyStateDisplayed()).toBe(true);
      expect(await harness.isErrorDisplayed()).toBe(false);
    });
  });

  describe('error state', () => {
    it('should display error card when the request fails', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      httpTestingController
        .expectOne((r) => r.url === BASE_URL && r.method === 'GET')
        .flush('Server error', { status: 500, statusText: 'Internal Server Error' });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.isChartDisplayed()).toBe(false);
      expect(await harness.isEmptyStateDisplayed()).toBe(false);
      expect(await harness.isErrorDisplayed()).toBe(true);
    });
  });

  describe('refresh on timeframe change', () => {
    it('should re-fetch when the time range filter changes', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushGroupByRequest();
      fixture.detectChanges();

      expect(await harness.isChartDisplayed()).toBe(true);

      const newFrom = TO;
      const newTo = TO + 3_600_000;
      analyticsService.setTimeRangeFilter({ from: newFrom, to: newTo });
      fixture.detectChanges();

      // Shows loading again while new request is in flight
      expect(await harness.isLoading()).toBe(true);

      const req = httpTestingController.expectOne((r) => r.url === BASE_URL && r.method === 'GET');
      expect(+req.request.params.get('from')).toBe(newFrom);
      expect(+req.request.params.get('to')).toBe(newTo);
      req.flush(fakeGroupByResponse({ values: { '200': 50 } }));
      fixture.detectChanges();

      expect(await harness.isChartDisplayed()).toBe(true);
    });
  });

  // ── helper ──────────────────────────────────────────────────────────────────

  function flushGroupByRequest(body = fakeGroupByResponse()) {
    const req = httpTestingController.expectOne((r) => r.url === BASE_URL && r.method === 'GET');
    req.flush(body);
  }
});
