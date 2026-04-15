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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ApiAnalyticsStatsCardsComponent } from './api-analytics-stats-cards.component';
import { ApiAnalyticsStatsCardsHarness } from './api-analytics-stats-cards.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { fakeCountResponse, fakeStatsResponse } from '../../../../../../entities/management-api-v2/analytics/analyticsResponse.fixture';

const API_ID = 'test-api-id';
const BASE_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics`;
const FROM = 1_700_000_000_000;
const TO = 1_700_086_400_000;

describe('ApiAnalyticsStatsCardsComponent', () => {
  let fixture: ComponentFixture<ApiAnalyticsStatsCardsComponent>;
  let httpTestingController: HttpTestingController;
  let harness: ApiAnalyticsStatsCardsHarness;
  let analyticsService: ApiAnalyticsV2Service;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsStatsCardsComponent, GioTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    analyticsService = TestBed.inject(ApiAnalyticsV2Service);

    fixture = TestBed.createComponent(ApiAnalyticsStatsCardsComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsStatsCardsHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('loading state', () => {
    it('should show loading skeleton for all four cards before data is fetched', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();

      expect(await harness.isCardLoading('Total Requests')).toBe(true);
      expect(await harness.isCardLoading('Avg Gateway Response Time')).toBe(true);
      expect(await harness.isCardLoading('Avg Upstream Response Time')).toBe(true);
      expect(await harness.isCardLoading('Avg Content Length')).toBe(true);

      flushAllRequests();
    });
  });

  describe('populated state', () => {
    it('should display Total Requests count from COUNT response', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushAllRequests({ count: fakeCountResponse({ count: 1500 }) });
      fixture.detectChanges();

      expect(await harness.isCardLoading('Total Requests')).toBe(false);
      const value = await harness.getCardValue('Total Requests');
      expect(value).toContain('1,500');
    });

    it('should display Avg Gateway Response Time from STATS response formatted as ms', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushAllRequests({ gateway: fakeStatsResponse({ avg: 123.7 }) });
      fixture.detectChanges();

      expect(await harness.isCardLoading('Avg Gateway Response Time')).toBe(false);
      const value = await harness.getCardValue('Avg Gateway Response Time');
      expect(value).toContain('124 ms');
    });

    it('should display Avg Upstream Response Time from STATS response formatted as ms', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushAllRequests({ upstream: fakeStatsResponse({ avg: 45.2 }) });
      fixture.detectChanges();

      expect(await harness.isCardLoading('Avg Upstream Response Time')).toBe(false);
      const value = await harness.getCardValue('Avg Upstream Response Time');
      expect(value).toContain('45 ms');
    });

    it('should display Avg Content Length as human-readable bytes', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushAllRequests({ contentLength: fakeStatsResponse({ avg: 4301 }) });
      fixture.detectChanges();

      expect(await harness.isCardLoading('Avg Content Length')).toBe(false);
      const value = await harness.getCardValue('Avg Content Length');
      expect(value).toContain('KB');
    });

    it('should render four cards in total', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushAllRequests();
      fixture.detectChanges();

      expect(await harness.getCardCount()).toBe(4);
    });
  });

  describe('empty state', () => {
    it('should show en-dash for Total Requests when count is 0', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushAllRequests({ count: fakeCountResponse({ count: 0 }) });
      fixture.detectChanges();

      expect(await harness.isCardEmpty('Total Requests')).toBe(true);
      expect(await harness.isCardError('Total Requests')).toBe(false);
    });

    it('should show en-dash for STATS cards when count is 0', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushAllRequests({
        gateway: fakeStatsResponse({ count: 0, avg: 0 }),
        upstream: fakeStatsResponse({ count: 0, avg: 0 }),
        contentLength: fakeStatsResponse({ count: 0, avg: 0 }),
      });
      fixture.detectChanges();

      expect(await harness.isCardEmpty('Avg Gateway Response Time')).toBe(true);
      expect(await harness.isCardEmpty('Avg Upstream Response Time')).toBe(true);
      expect(await harness.isCardEmpty('Avg Content Length')).toBe(true);
    });
  });

  describe('error state', () => {
    it('should show error icon for a card whose request fails', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushCountWithError();
      flushGatewayRequest();
      flushUpstreamRequest();
      flushContentLengthRequest();
      fixture.detectChanges();

      expect(await harness.isCardError('Total Requests')).toBe(true);
    });

    it('should display data for other cards when one fails (partial failure)', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushCountWithError();
      flushGatewayRequest(fakeStatsResponse({ avg: 100 }));
      flushUpstreamRequest(fakeStatsResponse({ avg: 50 }));
      flushContentLengthRequest(fakeStatsResponse({ avg: 2048 }));
      fixture.detectChanges();

      expect(await harness.isCardError('Total Requests')).toBe(true);
      expect(await harness.isCardLoading('Avg Gateway Response Time')).toBe(false);
      expect(await harness.isCardError('Avg Gateway Response Time')).toBe(false);
      const gatewayValue = await harness.getCardValue('Avg Gateway Response Time');
      expect(gatewayValue).toContain('100 ms');
    });
  });

  describe('refresh on timeframe change', () => {
    it('should re-fetch all cards when the time range filter changes', async () => {
      analyticsService.setTimeRangeFilter({ from: FROM, to: TO });
      fixture.detectChanges();
      flushAllRequests();
      fixture.detectChanges();

      // Change the time range
      const newFrom = TO;
      const newTo = TO + 3_600_000;
      analyticsService.setTimeRangeFilter({ from: newFrom, to: newTo });
      fixture.detectChanges();

      // Expect 4 new requests with the updated time range
      const requests = httpTestingController.match((req) => req.url === BASE_URL);
      expect(requests.length).toBe(4);
      requests.forEach((req) => {
        expect(+req.request.params.get('from')).toBe(newFrom);
        expect(+req.request.params.get('to')).toBe(newTo);
        req.flush(fakeCountResponse());
      });
    });
  });

  // ── helpers ─────────────────────────────────────────────────────────────────

  function flushAllRequests(
    overrides: {
      count?: ReturnType<typeof fakeCountResponse>;
      gateway?: ReturnType<typeof fakeStatsResponse>;
      upstream?: ReturnType<typeof fakeStatsResponse>;
      contentLength?: ReturnType<typeof fakeStatsResponse>;
    } = {},
  ) {
    flushCountRequest(overrides.count);
    flushGatewayRequest(overrides.gateway);
    flushUpstreamRequest(overrides.upstream);
    flushContentLengthRequest(overrides.contentLength);
  }

  function flushCountRequest(body?: ReturnType<typeof fakeCountResponse>) {
    const req = httpTestingController.expectOne((r) => r.url === BASE_URL && r.method === 'GET' && r.params.get('type') === 'COUNT');
    req.flush(body ?? fakeCountResponse());
  }

  function flushCountWithError() {
    const req = httpTestingController.expectOne((r) => r.url === BASE_URL && r.method === 'GET' && r.params.get('type') === 'COUNT');
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  }

  function flushGatewayRequest(body?: ReturnType<typeof fakeStatsResponse>) {
    const req = httpTestingController.expectOne(
      (r) =>
        r.url === BASE_URL &&
        r.method === 'GET' &&
        r.params.get('type') === 'STATS' &&
        r.params.get('field') === 'gateway-response-time-ms',
    );
    req.flush(body ?? fakeStatsResponse());
  }

  function flushUpstreamRequest(body?: ReturnType<typeof fakeStatsResponse>) {
    const req = httpTestingController.expectOne(
      (r) =>
        r.url === BASE_URL &&
        r.method === 'GET' &&
        r.params.get('type') === 'STATS' &&
        r.params.get('field') === 'endpoint-response-time-ms',
    );
    req.flush(body ?? fakeStatsResponse());
  }

  function flushContentLengthRequest(body?: ReturnType<typeof fakeStatsResponse>) {
    const req = httpTestingController.expectOne(
      (r) =>
        r.url === BASE_URL && r.method === 'GET' && r.params.get('type') === 'STATS' && r.params.get('field') === 'request-content-length',
    );
    req.flush(body ?? fakeStatsResponse());
  }
});
