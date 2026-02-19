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

import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { HttpTestingController } from '@angular/common/http/testing';

import { EnvLogsComponent } from './env-logs.component';
import { EnvLogsTableHarness } from './components/env-logs-table/env-logs-table.harness';
import { EnvLogsFilterBarHarness } from './components/env-logs-filter-bar/env-logs-filter-bar.harness';

import { GioTestingModule, CONSTANTS_TESTING } from '../../../shared/testing/gio-testing.module';
import { SearchLogsResponse } from '../../../services-ngx/environment-logs.service';

describe('EnvLogsComponent', () => {
  let component: EnvLogsComponent;
  let fixture: ComponentFixture<EnvLogsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const SEARCH_URL = `${CONSTANTS_TESTING.env.v2BaseURL}/logs/search?page=1&perPage=10`;

  const EMPTY_RESPONSE: SearchLogsResponse = {
    data: [],
    pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
  };

  const MOCK_RESPONSE: SearchLogsResponse = {
    data: [
      {
        apiId: 'api-1',
        timestamp: '2025-06-15T12:00:00Z',
        id: 'log-1',
        requestId: 'req-1',
        method: 'GET',
        status: 200,
        requestEnded: true,
        gatewayResponseTime: 44,
        gateway: 'gw-uuid-1',
        uri: '/poke',
        plan: { id: 'plan-1' },
        application: { id: 'app-1' },
      },
    ],
    pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatCardModule, GioBannerModule, EnvLogsComponent],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EnvLogsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpTestingController.verify();
    fixture.destroy();
  });

  function flushSearchAndResolution(
    response: SearchLogsResponse,
    overrides?: { failApp?: boolean; failPlan?: boolean; failGateway?: boolean },
    searchUrl: string = SEARCH_URL,
  ) {
    // Flush the search request
    const req = httpTestingController.expectOne({ method: 'POST', url: searchUrl });
    req.flush(response);

    // Flush API name resolution requests
    const apiIds = [...new Set(response.data.map(log => log.apiId).filter(Boolean))];
    for (const apiId of apiIds) {
      const apiReq = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`,
      });
      apiReq.flush({ id: apiId, name: `Resolved ${apiId}` });
    }

    // Flush application name resolution requests (excluding keyless app '1')
    const appIds = [...new Set(response.data.map(log => log.application?.id).filter(id => Boolean(id) && id !== '1'))];
    for (const appId of appIds) {
      const appReq = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${appId}`,
      });
      if (overrides?.failApp) {
        appReq.flush('Not Found', { status: 404, statusText: 'Not Found' });
      } else {
        appReq.flush({ id: appId, name: `Resolved ${appId}` });
      }
    }

    // Flush gateway name resolution requests
    const gatewayIds = [...new Set(response.data.map(log => log.gateway).filter(Boolean))];
    for (const gwId of gatewayIds) {
      const gwReq = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/instances/${gwId}`,
      });
      if (overrides?.failGateway) {
        gwReq.flush('Not Found', { status: 404, statusText: 'Not Found' });
      } else {
        gwReq.flush({ id: gwId, hostname: `Resolved ${gwId}` });
      }
    }

    // Flush plan name resolution requests
    const planEntries = [
      ...new Map(
        response.data.filter(log => log.plan?.id && log.apiId).map(log => [log.plan.id, { apiId: log.apiId, planId: log.plan.id }]),
      ).values(),
    ];
    for (const { apiId, planId } of planEntries) {
      const planReq = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans/${planId}`,
      });
      if (overrides?.failPlan) {
        planReq.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
      } else {
        planReq.flush({ id: planId, name: `Resolved ${planId}` });
      }
    }

    // Re-render with fetched data
    fixture.detectChanges();
  }

  function initComponent(
    response: SearchLogsResponse = EMPTY_RESPONSE,
    overrides?: { failApp?: boolean; failPlan?: boolean; failGateway?: boolean },
  ) {
    fixture.detectChanges();
    tick();
    flushSearchAndResolution(response, overrides);
  }

  it('should create', fakeAsync(() => {
    initComponent();
    expect(component).toBeTruthy();
  }));

  it('should render the title', fakeAsync(() => {
    initComponent();

    const title = fixture.nativeElement.querySelector('h1');
    expect(title.textContent).toContain('Logs');
  }));

  it('should render the banner warning', fakeAsync(() => {
    initComponent();

    const banner = fixture.nativeElement.querySelector('gio-banner-info');
    expect(banner).toBeTruthy();
  }));

  it('should display filters section', fakeAsync(async () => {
    initComponent();

    const filterBar = await loader.getHarness(EnvLogsFilterBarHarness);
    expect(filterBar).toBeTruthy();
  }));

  it('should display table section', fakeAsync(async () => {
    initComponent();

    const tableSection = await loader.getHarness(EnvLogsTableHarness);
    expect(tableSection).toBeTruthy();
  }));

  it('should fetch logs on init and resolve API, application, plan, and gateway names', fakeAsync(() => {
    initComponent(MOCK_RESPONSE);

    const logs = component.logs();
    expect(logs.length).toBe(1);
    expect(logs[0].api).toBe('Resolved api-1');
    expect(logs[0].method).toBe('GET');
    expect(logs[0].status).toBe(200);
    expect(logs[0].path).toBe('/poke');
    expect(logs[0].application).toBe('Resolved app-1');
    expect(logs[0].responseTime).toBe('44 ms');
    expect(logs[0].gateway).toBe('Resolved gw-uuid-1');
    expect(logs[0].plan?.name).toBe('Resolved plan-1');
    expect(logs[0].requestEnded).toBe(true);
  }));

  it('should fall back to ID when name resolution fails', fakeAsync(() => {
    initComponent(MOCK_RESPONSE, { failApp: true, failPlan: true, failGateway: true });

    const logs = component.logs();
    expect(logs.length).toBe(1);
    expect(logs[0].application).toBe('app-1');
    expect(logs[0].plan?.name).toBe('api-1|plan-1');
    expect(logs[0].gateway).toBe('gw-uuid-1');
  }));

  it('should display "Unknown application (keyless)" for default application ID', fakeAsync(() => {
    const responseWithDefaultApp: SearchLogsResponse = {
      data: [
        {
          apiId: 'api-1',
          timestamp: '2025-06-15T12:00:00Z',
          id: 'log-2',
          requestId: 'req-2',
          method: 'GET',
          status: 200,
          requestEnded: true,
          gateway: 'gw-uuid-1',
          uri: '/test',
          application: { id: '1' },
        },
      ],
      pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
    };

    initComponent(responseWithDefaultApp);

    // No application HTTP request should have been made (verified by httpTestingController.verify() in afterEach)
    const logs = component.logs();
    expect(logs[0].application).toBe('Unknown application (keyless)');
  }));

  it('should update pagination from response', fakeAsync(() => {
    initComponent(MOCK_RESPONSE);

    expect(component.paginationWithTotal().totalCount).toBe(1);
  }));

  it('should show empty table when no logs returned', fakeAsync(() => {
    initComponent();

    expect(component.logs().length).toBe(0);
    expect(component.loading()).toBe(false);
  }));

  it('should trigger new search on pagination update', fakeAsync(() => {
    initComponent(MOCK_RESPONSE);

    component.onPaginationUpdated({ index: 2, size: 10 });
    fixture.detectChanges();
    tick(1);

    const page2Url = `${CONSTANTS_TESTING.env.v2BaseURL}/logs/search?page=2&perPage=10`;
    flushSearchAndResolution(EMPTY_RESPONSE, undefined, page2Url);

    expect(component.logs().length).toBe(0);
  }));

  it('should reset to page 1 on refresh', fakeAsync(() => {
    initComponent(MOCK_RESPONSE);

    component.onPaginationUpdated({ index: 2, size: 10 });
    fixture.detectChanges();
    tick(1);

    const page2Url = `${CONSTANTS_TESTING.env.v2BaseURL}/logs/search?page=2&perPage=10`;
    flushSearchAndResolution(EMPTY_RESPONSE, undefined, page2Url);

    component.onRefresh();
    fixture.detectChanges();
    tick(1);

    flushSearchAndResolution(MOCK_RESPONSE);

    expect(component.logs().length).toBe(1);
    expect(component.pagination().page).toBe(1);
  }));

  it('should display error when searchLogs fails', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne({ method: 'POST', url: SEARCH_URL });
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    expect(component.error()).toBe('Request failed: 500 Internal Server Error');
    expect(component.loading()).toBe(false);
    expect(component.logs().length).toBe(0);
  }));

  it('should deduplicate IDs across multiple log entries', fakeAsync(() => {
    const multiEntryResponse: SearchLogsResponse = {
      data: [
        {
          apiId: 'api-1',
          timestamp: '2025-06-15T12:00:00Z',
          id: 'log-1',
          requestId: 'req-1',
          method: 'GET',
          status: 200,
          requestEnded: true,
          uri: '/a',
          application: { id: 'app-1' },
          plan: { id: 'plan-1' },
          gateway: 'gw-1',
        },
        {
          apiId: 'api-1',
          timestamp: '2025-06-15T12:01:00Z',
          id: 'log-2',
          requestId: 'req-2',
          method: 'POST',
          status: 201,
          requestEnded: true,
          uri: '/b',
          application: { id: 'app-1' },
          plan: { id: 'plan-1' },
          gateway: 'gw-1',
        },
      ],
      pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 2, totalCount: 2 },
    };

    initComponent(multiEntryResponse);

    // If deduplication works, only 1 API, 1 app, 1 plan, and 1 gateway request
    // (verified by httpTestingController.verify() in afterEach)
    const logs = component.logs();
    expect(logs.length).toBe(2);
    expect(logs[0].api).toBe('Resolved api-1');
    expect(logs[1].api).toBe('Resolved api-1');
    expect(logs[0].application).toBe('Resolved app-1');
    expect(logs[1].application).toBe('Resolved app-1');
  }));
});
