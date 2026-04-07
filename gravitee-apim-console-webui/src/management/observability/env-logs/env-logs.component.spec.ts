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
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { HttpTestingController } from '@angular/common/http/testing';

import { EnvLogsComponent, EnvLogsTab } from './env-logs.component';
import { EnvLogsTableHarness } from './components/env-logs-table/env-logs-table.harness';
import { EnvLogsFilterBarHarness } from './components/env-logs-filter-bar/env-logs-filter-bar.harness';

import { GioTestingModule, CONSTANTS_TESTING } from '../../../shared/testing/gio-testing.module';
import { EnvironmentApiLog, SearchLogsResponse } from '../../../services-ngx/environment-logs.service';

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
        apiName: 'My API',
        timestamp: '2025-06-15T12:00:00Z',
        id: 'log-1',
        requestId: 'req-1',
        method: 'GET',
        status: 200,
        requestEnded: true,
        gatewayResponseTime: 44,
        gateway: 'gateway-host-1',
        uri: '/poke',
        plan: { id: 'plan-1', name: 'Gold Plan' },
        application: { id: 'app-1', name: 'My App' },
      },
    ],
    pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatCardModule, MatTabsModule, GioBannerModule, EnvLogsComponent],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EnvLogsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    flushErrorKeys();
    httpTestingController.verify();
    fixture.destroy();
  });

  function flushErrorKeys() {
    const reqs = httpTestingController.match(req => req.url.includes('/analytics/error-keys'));
    reqs.forEach(req => req.flush([]));
  }

  function flushSearch(response: SearchLogsResponse, searchUrl: string = SEARCH_URL) {
    const req = httpTestingController.expectOne({ method: 'POST', url: searchUrl });
    req.flush(response);
    fixture.detectChanges();
  }

  function initComponent(response: SearchLogsResponse = EMPTY_RESPONSE) {
    fixture.detectChanges();
    tick();
    flushSearch(response);
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

  it('should render two tabs', fakeAsync(() => {
    initComponent();

    const tabs = fixture.nativeElement.querySelectorAll('.mat-mdc-tab');
    expect(tabs.length).toBe(2);
    expect(tabs[0].textContent).toContain('All non-message APIs');
    expect(tabs[1].textContent).toContain('Messages and Events');
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

  it('should render Asset Type column with mapped label in the table', fakeAsync(async () => {
    const response: SearchLogsResponse = {
      data: [
        {
          apiId: 'api-1',
          apiName: 'My API',
          apiType: 'LLM_PROXY',
          timestamp: '2025-06-15T12:00:00Z',
          id: 'log-1',
          requestId: 'req-1',
          method: 'GET',
          status: 200,
          requestEnded: true,
          uri: '/test',
        },
      ],
      pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
    };

    initComponent(response);

    const table = await loader.getHarness(EnvLogsTableHarness);
    const rows = await table.getRowsData();
    expect(rows.length).toBe(1);
    expect(rows[0]['assetType']).toContain('LLM');
  }));

  it('should render dash in Asset Type column when apiType is absent', fakeAsync(async () => {
    initComponent(MOCK_RESPONSE);

    const table = await loader.getHarness(EnvLogsTableHarness);
    const rows = await table.getRowsData();
    expect(rows.length).toBe(1);
    expect(rows[0]['assetType']).toContain('—');
  }));

  it('should fetch logs on init and use backend-provided names', fakeAsync(() => {
    initComponent(MOCK_RESPONSE);

    const logs = component.logs();
    expect(logs.length).toBe(1);
    expect(logs[0].api).toBe('My API');
    expect(logs[0].method).toBe('GET');
    expect(logs[0].status).toBe(200);
    expect(logs[0].path).toBe('/poke');
    expect(logs[0].application).toBe('My App');
    expect(logs[0].responseTime).toBe('44 ms');
    expect(logs[0].gateway).toBe('gateway-host-1');
    expect(logs[0].plan?.name).toBe('Gold Plan');
    expect(logs[0].requestEnded).toBe(true);
  }));

  it('should fall back to ID when name is absent in the response', fakeAsync(() => {
    const responseWithoutNames: SearchLogsResponse = {
      data: [
        {
          apiId: 'api-1',
          timestamp: '2025-06-15T12:00:00Z',
          id: 'log-1',
          requestId: 'req-1',
          method: 'GET',
          status: 200,
          requestEnded: true,
          uri: '/poke',
          plan: { id: 'plan-1' },
          application: { id: 'app-1' },
        },
      ],
      pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
    };

    initComponent(responseWithoutNames);

    const logs = component.logs();
    expect(logs.length).toBe(1);
    expect(logs[0].api).toBe('api-1');
    expect(logs[0].application).toBe('app-1');
    expect(logs[0].plan).toBeUndefined();
    expect(logs[0].gateway).toBeUndefined();
  }));

  it('should display application ID when name is not provided', fakeAsync(() => {
    const responseWithDefaultApp: SearchLogsResponse = {
      data: [
        {
          apiId: 'api-1',
          apiName: 'My API',
          timestamp: '2025-06-15T12:00:00Z',
          id: 'log-2',
          requestId: 'req-2',
          method: 'GET',
          status: 200,
          requestEnded: true,
          gateway: 'gateway-host-1',
          uri: '/test',
          application: { id: '1' },
        },
      ],
      pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
    };

    initComponent(responseWithDefaultApp);

    const logs = component.logs();
    expect(logs[0].application).toBe('1');
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
    flushSearch(EMPTY_RESPONSE, page2Url);

    expect(component.logs().length).toBe(0);
  }));

  it('should reset to page 1 on refresh', fakeAsync(() => {
    initComponent(MOCK_RESPONSE);

    component.onPaginationUpdated({ index: 2, size: 10 });
    fixture.detectChanges();
    tick(1);

    const page2Url = `${CONSTANTS_TESTING.env.v2BaseURL}/logs/search?page=2&perPage=10`;
    flushSearch(EMPTY_RESPONSE, page2Url);

    component.onRefresh();
    fixture.detectChanges();
    tick(1);

    flushSearch(MOCK_RESPONSE);

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

  it('should display multiple log entries from response', fakeAsync(() => {
    const multiEntryResponse: SearchLogsResponse = {
      data: [
        {
          apiId: 'api-1',
          apiName: 'My API',
          timestamp: '2025-06-15T12:00:00Z',
          id: 'log-1',
          requestId: 'req-1',
          method: 'GET',
          status: 200,
          requestEnded: true,
          uri: '/a',
          application: { id: 'app-1', name: 'My App' },
          plan: { id: 'plan-1', name: 'Gold Plan' },
          gateway: 'gateway-host-1',
        },
        {
          apiId: 'api-1',
          apiName: 'My API',
          timestamp: '2025-06-15T12:01:00Z',
          id: 'log-2',
          requestId: 'req-2',
          method: 'POST',
          status: 201,
          requestEnded: true,
          uri: '/b',
          application: { id: 'app-1', name: 'My App' },
          plan: { id: 'plan-1', name: 'Gold Plan' },
          gateway: 'gateway-host-1',
        },
      ],
      pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 2, totalCount: 2 },
    };

    initComponent(multiEntryResponse);

    const logs = component.logs();
    expect(logs.length).toBe(2);
    expect(logs[0].api).toBe('My API');
    expect(logs[1].api).toBe('My API');
    expect(logs[0].application).toBe('My App');
    expect(logs[1].application).toBe('My App');
  }));

  describe('mapToEnvLog edge cases', () => {
    it('should display dashes when method, uri, and gatewayResponseTime are missing', fakeAsync(() => {
      const response: SearchLogsResponse = {
        data: [
          {
            apiId: 'api-1',
            apiName: 'Test API',
            timestamp: '2025-06-15T12:00:00Z',
            id: 'log-missing',
            requestId: 'req-m',
            status: 200,
            requestEnded: true,
          } as EnvironmentApiLog,
        ],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      };

      initComponent(response);

      const log = component.logs()[0];
      expect(log.method).toBe('—');
      expect(log.path).toBe('—');
      expect(log.responseTime).toBe('—');
    }));

    it('should display dash when application object is missing entirely', fakeAsync(() => {
      const response: SearchLogsResponse = {
        data: [
          {
            apiId: 'api-1',
            apiName: 'Test API',
            timestamp: '2025-06-15T12:00:00Z',
            id: 'log-no-app',
            requestId: 'req-na',
            method: 'GET',
            status: 200,
            requestEnded: true,
            uri: '/test',
          },
        ],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      };

      initComponent(response);

      expect(component.logs()[0].application).toBe('—');
    }));

    it('should map errorKey and warnings from response', fakeAsync(() => {
      const response: SearchLogsResponse = {
        data: [
          {
            apiId: 'api-1',
            apiName: 'Test API',
            timestamp: '2025-06-15T12:00:00Z',
            id: 'log-errors',
            requestId: 'req-e',
            method: 'GET',
            status: 502,
            requestEnded: false,
            uri: '/fail',
            errorKey: 'CONNECTION_TIMEOUT',
            warnings: [{ key: 'SLOW_RESPONSE' }, { key: 'DEPRECATED_HEADER' }],
          },
        ],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      };

      initComponent(response);

      const log = component.logs()[0];
      expect(log.errorKey).toBe('CONNECTION_TIMEOUT');
      expect(log.requestEnded).toBe(false);
      expect(log.warnings).toEqual([{ key: 'SLOW_RESPONSE' }, { key: 'DEPRECATED_HEADER' }]);
    }));

    it.each([
      ['HTTP_PROXY', 'HTTP'],
      ['LLM_PROXY', 'LLM'],
      ['MCP_PROXY', 'MCP'],
    ] as const)(
      'should map apiType %s to display label %s',
      fakeAsync((backendType, displayLabel) => {
        const response: SearchLogsResponse = {
          data: [
            {
              apiId: 'api-1',
              apiName: 'Test API',
              apiType: backendType,
              timestamp: '2025-06-15T12:00:00Z',
              id: 'log-typed',
              requestId: 'req-typed',
              method: 'GET',
              status: 200,
              requestEnded: true,
              uri: '/test',
            },
          ],
          pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
        };

        initComponent(response);

        expect(component.logs()[0].apiType).toBe(displayLabel);
      }),
    );

    it('should pass through unrecognized apiType values as-is', fakeAsync(() => {
      const response: SearchLogsResponse = {
        data: [
          {
            apiId: 'api-1',
            apiName: 'Test API',
            apiType: 'FUTURE_TYPE' as any,
            timestamp: '2025-06-15T12:00:00Z',
            id: 'log-unknown',
            requestId: 'req-unknown',
            method: 'GET',
            status: 200,
            requestEnded: true,
            uri: '/test',
          },
        ],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      };

      initComponent(response);

      expect(component.logs()[0].apiType).toBe('FUTURE_TYPE');
    }));

    it('should leave apiType undefined when not provided in response', fakeAsync(() => {
      initComponent(MOCK_RESPONSE);

      expect(component.logs()[0].apiType).toBeUndefined();
    }));
  });

  describe('tab infrastructure', () => {
    it('should default to first tab (non-message APIs)', fakeAsync(() => {
      initComponent();

      expect(component.activeTab()).toBe(EnvLogsTab.NON_MESSAGE_APIS);
    }));

    it('should switch to messages tab and reset pagination', fakeAsync(() => {
      initComponent(MOCK_RESPONSE);

      component.onPaginationUpdated({ index: 3, size: 10 });
      fixture.detectChanges();
      tick(1);

      const page3Url = `${CONSTANTS_TESTING.env.v2BaseURL}/logs/search?page=3&perPage=10`;
      flushSearch(EMPTY_RESPONSE, page3Url);

      component.onTabChanged(1);
      fixture.detectChanges();
      tick(1);

      // Tab change resets pagination to page 1, which re-triggers the search pipeline.
      // Drain all pending search requests from the debounce cascade.
      let pending = httpTestingController.match(req => req.method === 'POST' && req.url.includes('/logs/search'));
      pending.forEach(req => req.flush(EMPTY_RESPONSE));
      fixture.detectChanges();
      tick(1);
      pending = httpTestingController.match(req => req.method === 'POST' && req.url.includes('/logs/search'));
      pending.forEach(req => req.flush(EMPTY_RESPONSE));

      expect(component.activeTab()).toBe(EnvLogsTab.MESSAGES);
      expect(component.pagination().page).toBe(1);
      expect(component.messagePagination().page).toBe(1);
    }));

    it('should update message pagination independently', fakeAsync(() => {
      initComponent();

      component.onMessagePaginationUpdated({ index: 2, size: 25 });

      expect(component.messagePagination().page).toBe(2);
      expect(component.messagePagination().perPage).toBe(25);
      // Non-message pagination should be unaffected
      expect(component.pagination().page).toBe(1);
      expect(component.pagination().perPage).toBe(10);
    }));
  });
});

describe('EnvLogsComponent — query param persistence', () => {
  let component: EnvLogsComponent;
  let fixture: ComponentFixture<EnvLogsComponent>;
  let httpTestingController: HttpTestingController;

  function createComponentWithQueryParams(queryParams: Record<string, string>) {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatCardModule, MatTabsModule, GioBannerModule, EnvLogsComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParams },
            queryParams: of(queryParams),
          },
        },
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EnvLogsComponent);
    component = fixture.componentInstance;
  }

  function flushErrorKeys() {
    const reqs = httpTestingController.match(req => req.url.includes('/analytics/error-keys'));
    reqs.forEach(req => req.flush([]));
  }

  afterEach(() => {
    flushErrorKeys();
    httpTestingController.verify();
    fixture?.destroy();
  });

  it('should restore filters from query params', fakeAsync(() => {
    createComponentWithQueryParams({
      period: '-7d',
      methods: 'GET,POST',
      statuses: '200,404',
      apiIds: 'api-1,api-2',
    });

    fixture.detectChanges();
    tick();

    const searchReq = httpTestingController.expectOne(req => req.method === 'POST' && req.url.includes('/logs/search'));
    const body = searchReq.request.body;

    expect(body.filters).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: 'HTTP_METHOD', operator: 'IN', value: ['GET', 'POST'] }),
        expect.objectContaining({ name: 'API', operator: 'IN', value: ['api-1', 'api-2'] }),
        expect.objectContaining({ name: 'HTTP_STATUS', operator: 'IN', value: ['200', '404'] }),
      ]),
    );
    searchReq.flush({ data: [], pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 } });
  }));

  it('should restore pagination from query params', fakeAsync(() => {
    createComponentWithQueryParams({
      perPage: '25',
      methods: 'GET',
    });

    fixture.detectChanges();
    tick();

    // Verify the pagination signal was updated with restored perPage
    expect(component.pagination().perPage).toBe(25);

    // Flush any pending search requests
    const searchReqs = httpTestingController.match(req => req.method === 'POST' && req.url.includes('/logs/search'));
    searchReqs.forEach(req =>
      req.flush({ data: [], pagination: { page: 1, perPage: 25, pageCount: 0, pageItemsCount: 0, totalCount: 0 } }),
    );
  }));

  it('should return undefined when no filter query params exist', fakeAsync(() => {
    createComponentWithQueryParams({});

    fixture.detectChanges();
    tick();

    // With no query params, parseQueryParams returns undefined and default period is used
    const searchReq = httpTestingController.expectOne(req => req.method === 'POST' && req.url.includes('/logs/search'));
    searchReq.flush({ data: [], pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 } });
  }));

  it('should restore more filter values from query params', fakeAsync(() => {
    createComponentWithQueryParams({
      transactionId: 'txn-123',
      requestId: 'req-456',
      uri: '/api/test',
      responseTime: '500',
      methods: 'GET',
    });

    fixture.detectChanges();
    tick();

    const searchReq = httpTestingController.expectOne(req => req.method === 'POST' && req.url.includes('/logs/search'));
    const body = searchReq.request.body;

    expect(body.filters).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: 'TRANSACTION_ID', value: 'txn-123' }),
        expect.objectContaining({ name: 'REQUEST_ID', value: 'req-456' }),
        expect.objectContaining({ name: 'URI', value: '/api/test' }),
        expect.objectContaining({ name: 'RESPONSE_TIME', operator: 'GTE', value: 500 }),
      ]),
    );
    searchReq.flush({ data: [], pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 } });
  }));

  it('should restore messages tab from query params', fakeAsync(() => {
    createComponentWithQueryParams({ tab: 'messages' });

    fixture.detectChanges();
    tick();

    expect(component.activeTab()).toBe(EnvLogsTab.MESSAGES);

    const searchReq = httpTestingController.expectOne(req => req.method === 'POST' && req.url.includes('/logs/search'));
    searchReq.flush({ data: [], pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 } });
  }));

  it('should default to first tab when no tab query param', fakeAsync(() => {
    createComponentWithQueryParams({});

    fixture.detectChanges();
    tick();

    expect(component.activeTab()).toBe(EnvLogsTab.NON_MESSAGE_APIS);

    const searchReq = httpTestingController.expectOne(req => req.method === 'POST' && req.url.includes('/logs/search'));
    searchReq.flush({ data: [], pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 } });
  }));
});
