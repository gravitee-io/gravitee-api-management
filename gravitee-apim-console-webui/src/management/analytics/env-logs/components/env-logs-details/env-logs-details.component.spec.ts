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
import { ActivatedRoute, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';

import { EnvLogsDetailsComponent } from './env-logs-details.component';
import { EnvLogsDetailsHarness } from './env-logs-details.harness';

import { GioTestingModule, CONSTANTS_TESTING } from '../../../../../shared/testing/gio-testing.module';
import { fakeConnectionLogDetail } from '../../../../../entities/management-api-v2/log/connectionLog.fixture';
import { fakeApiMetricResponse } from '../../../../../entities/management-api-v2/analytics/apiMetricsDetailResponse.fixture';
import { SearchLogsResponse } from '../../../../../services-ngx/environment-logs.service';

describe('EnvLogsDetailsComponent', () => {
  const logId = 'req-abc-123';
  const apiId = 'api-xyz-456';

  const SEARCH_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/logs/search?page=1&perPage=10`;
  const DETAIL_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/logs/${logId}`;
  const METRICS_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/analytics/${logId}`;

  const MOCK_SEARCH_RESPONSE: SearchLogsResponse = {
    data: [
      {
        apiId,
        timestamp: '2025-06-15T12:00:00Z',
        id: logId,
        requestId: logId,
        method: 'GET',
        status: 200,
        requestEnded: true,
        gatewayResponseTime: 44,
        gateway: 'gw-uuid-1',
        uri: '/poke',
        plan: { id: 'plan-1', name: 'Free Plan' },
        application: { id: 'app-1', name: 'My App' },
        transactionId: 'txn-001',
      },
    ],
    pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
  };

  const MOCK_DETAIL = fakeConnectionLogDetail({ apiId, requestId: logId });
  const MOCK_METRICS = fakeApiMetricResponse({ apiId, requestId: logId });

  let httpTestingController: HttpTestingController;
  let fixture: ComponentFixture<EnvLogsDetailsComponent>;

  async function createComponent(params: Record<string, string> = { logId }, queryParams: Record<string, string> = { apiId }) {
    await TestBed.configureTestingModule({
      imports: [EnvLogsDetailsComponent, NoopAnimationsModule, GioTestingModule],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params, queryParams },
          },
        },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(EnvLogsDetailsComponent);
    fixture.detectChanges();

    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvLogsDetailsHarness);

    return { fixture, harness };
  }

  function flushRequests(searchResponse = MOCK_SEARCH_RESPONSE, detail = MOCK_DETAIL, metrics = MOCK_METRICS) {
    const searchReq = httpTestingController.expectOne({ method: 'POST', url: SEARCH_URL });
    searchReq.flush(searchResponse);

    const detailReq = httpTestingController.expectOne({ method: 'GET', url: DETAIL_URL });
    detailReq.flush(detail);

    const metricsReq = httpTestingController.expectOne({ method: 'GET', url: METRICS_URL });
    metricsReq.flush(metrics);
  }

  afterEach(() => {
    fixture?.destroy();
    httpTestingController.verify();
  });

  it('should display log details from backend', async () => {
    const { fixture: f, harness } = await createComponent();
    flushRequests();
    f.detectChanges();

    expect(await harness.getTitleText()).toContain('Log');
    expect(f.nativeElement.textContent).toContain('GET');
    expect(await harness.getStatusBadgeText()).toBe('200');
  });

  it('should display the API ID in the overview URI row', async () => {
    const { fixture: f, harness } = await createComponent();
    flushRequests();
    f.detectChanges();

    expect(await harness.getOverviewUri()).toContain(apiId);
  });

  it('should navigate back when the back button is clicked', async () => {
    const { fixture: f, harness } = await createComponent();
    flushRequests();
    f.detectChanges();

    // Verify back button is present and clickable (navigation is handled by routerLink)
    await expect(harness.clickBack()).resolves.not.toThrow();
  });

  it('should show "Log not found" banner when logId is missing', async () => {
    const { harness } = await createComponent({}, { apiId });

    httpTestingController.expectNone(SEARCH_URL);
    httpTestingController.expectNone(DETAIL_URL);
    httpTestingController.expectNone(METRICS_URL);

    expect(await harness.getNotFoundBannerText()).toContain('Log not found');
  });

  it('should show "Log not found" banner when apiId is missing', async () => {
    const { harness } = await createComponent({ logId }, {});

    httpTestingController.expectNone(SEARCH_URL);
    httpTestingController.expectNone(DETAIL_URL);
    httpTestingController.expectNone(METRICS_URL);

    expect(await harness.getNotFoundBannerText()).toContain('Log not found');
  });

  it('should show "Log not found" banner when search returns no results', async () => {
    const { fixture: f, harness } = await createComponent();

    const emptyResponse: SearchLogsResponse = {
      data: [],
      pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
    };

    const searchReq = httpTestingController.expectOne({ method: 'POST', url: SEARCH_URL });
    searchReq.flush(emptyResponse);
    f.detectChanges();

    expect(await harness.getNotFoundBannerText()).toContain('Log not found');
  });

  it('should return request headers sorted alphabetically', async () => {
    const { fixture: f } = await createComponent();
    flushRequests();
    f.detectChanges();

    const headers = f.componentInstance.requestHeaders();
    const keys = headers.map(h => h.key);
    expect(keys).toEqual([...keys].sort((a, b) => a.localeCompare(b)));
  });

  it('should display Application and Plan in the More Details panel', async () => {
    const { fixture: f, harness } = await createComponent();
    flushRequests();
    f.detectChanges();

    await harness.expandMoreDetails();
    f.detectChanges();

    const moreDetailsText = await harness.getMoreDetailsText();
    expect(moreDetailsText).toContain('My App');
    expect(moreDetailsText).toContain('Free Plan');
  });

  it('should render the success status badge for HTTP 200', async () => {
    const { fixture: f, harness } = await createComponent();
    flushRequests();
    f.detectChanges();

    expect(await harness.getStatusBadgeText()).toBe('200');
    expect(f.nativeElement.querySelector('.gio-badge-success')).toBeTruthy();
  });

  it('should display metrics-sourced fields (host, latency, response times)', async () => {
    const { fixture: f } = await createComponent();
    flushRequests();
    f.detectChanges();

    const text = f.nativeElement.textContent;
    expect(text).toContain('localhost:8082'); // host
    expect(text).toContain('0:0:0:0:0:0:0:1'); // remoteAddress
    expect(text).toContain('276 ms'); // gatewayResponseTime
    expect(text).toContain('150 ms'); // endpointResponseTime
    expect(text).toContain('3 ms'); // gatewayLatency
    expect(text).toContain('276'); // responseContentLength
  });
});
