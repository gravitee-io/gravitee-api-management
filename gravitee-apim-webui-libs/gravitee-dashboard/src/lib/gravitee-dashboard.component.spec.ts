/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';

import { MeasuresRequest } from './components/widget/model/request/measures-request';
import { RequestFilter, TimeRange } from './components/widget/model/request/request';
import { FacetsResponse } from './components/widget/model/response/facets-response';
import { MeasuresResponse } from './components/widget/model/response/measures-response';
import { Widget } from './components/widget/model/widget/widget.model';
import { GraviteeDashboardComponent } from './gravitee-dashboard.component';
import { Dashboard } from './models/dashboard.model';

const DEFAULT_TIME_RANGE: TimeRange = {
  from: '2025-01-01T00:00:00.000Z',
  to: '2025-01-01T01:00:00.000Z',
};

const EMPTY_DASHBOARD: Dashboard = {
  id: 'test-id',
  name: 'Test Dashboard',
  createdBy: 'user',
  createdAt: new Date().toISOString(),
  lastModified: new Date().toISOString(),
  labels: {},
  widgets: [],
};

const buildDashboard = (widgets: Widget[]): Dashboard => ({ ...EMPTY_DASHBOARD, widgets });

describe('GraviteeDashboardComponent', () => {
  let component: GraviteeDashboardComponent;
  let fixture: ComponentFixture<GraviteeDashboardComponent>;
  let httpTestingController: HttpTestingController;
  const mockBaseURL = 'http://customURL';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(GraviteeDashboardComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.componentRef.setInput('baseURL', mockBaseURL);
    fixture.componentRef.setInput('timeRange', DEFAULT_TIME_RANGE);
    fixture.componentRef.setInput('dashboard', EMPTY_DASHBOARD);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not load data for widgets without request', () => {
    const widgets: Widget[] = [
      {
        id: '1',
        title: 'Widget without request',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 0, y: 0 },
      },
    ];

    fixture.componentRef.setInput('dashboard', buildDashboard(widgets));
    fixture.detectChanges();

    const dashboardWidgets = component.dashboardWidgets();
    expect(dashboardWidgets.length).toBe(1);
    expect(dashboardWidgets[0].response).toBeUndefined();
  });

  it('should reload data for widgets even if they have existing response', fakeAsync(() => {
    const mockResponse: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          unit: 'NUMBER',
          measures: [{ name: 'COUNT', value: 100 }],
        },
      ],
    };

    const widgets: Widget[] = [
      {
        id: '1',
        title: 'Widget with response',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 0, y: 0 },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          metrics: [],
        },
        response: mockResponse,
      },
    ];

    fixture.componentRef.setInput('dashboard', buildDashboard(widgets));
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
    tick();

    const updatedWidgets = component.dashboardWidgets();
    expect(updatedWidgets.length).toBe(1);
    expect(updatedWidgets[0].response).toBeDefined();
    expect(updatedWidgets[0].response).toEqual(mockResponse);
  }));

  it('should load data for widgets with request but no response', fakeAsync(() => {
    const mockResponse: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          unit: 'NUMBER',
          measures: [{ name: 'AVG', value: 150 }],
        },
      ],
    };

    const widgets: Widget[] = [
      {
        id: '1',
        title: 'Widget to load',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 0, y: 0 },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          metrics: [],
        },
      },
    ];
    fixture.componentRef.setInput('dashboard', buildDashboard(widgets));
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
    tick();

    const updatedWidgets = component.dashboardWidgets();
    expect(updatedWidgets.length).toBe(1);
    expect(updatedWidgets[0].response).toBeDefined();
    expect(updatedWidgets[0].response).toEqual(mockResponse);
  }));

  it('should load data for widgets with facets request', fakeAsync(() => {
    const mockResponse: FacetsResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          unit: 'NUMBER',
          buckets: [
            { key: '2xx', name: '2xx', measures: [{ name: 'COUNT', value: 100 }] },
            { key: '4xx', name: '4xx', measures: [{ name: 'COUNT', value: 10 }] },
          ],
        },
      ],
    };

    const widgets: Widget[] = [
      {
        id: '1',
        title: 'Facets Widget',
        type: 'doughnut',
        layout: { cols: 1, rows: 1, x: 0, y: 0 },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [],
        },
      },
    ];

    fixture.componentRef.setInput('dashboard', buildDashboard(widgets));
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/facets`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
    tick();

    const updatedWidgets = component.dashboardWidgets();
    expect(updatedWidgets.length).toBe(1);
    expect(updatedWidgets[0].response).toBeDefined();
    expect(updatedWidgets[0].response).toEqual(mockResponse);
  }));

  it('should handle multiple widgets with different request types', fakeAsync(() => {
    const mockMeasuresResponse: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          unit: 'NUMBER',
          measures: [{ name: 'COUNT', value: 100 }],
        },
      ],
    };

    const mockFacetsResponse: FacetsResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          unit: 'NUMBER',
          buckets: [{ key: '2xx', name: '2xx', measures: [{ name: 'COUNT', value: 100 }] }],
        },
      ],
    };

    const widgets: Widget[] = [
      {
        id: '1',
        title: 'Measures Widget',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 0, y: 0 },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          metrics: [],
        },
      },
      {
        id: '2',
        title: 'Facets Widget',
        type: 'doughnut',
        layout: { cols: 1, rows: 1, x: 1, y: 0 },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [],
        },
      },
    ];

    fixture.componentRef.setInput('dashboard', buildDashboard(widgets));
    fixture.detectChanges();
    tick();

    const measuresReq = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    measuresReq.flush(mockMeasuresResponse);

    const facetsReq = httpTestingController.expectOne(`${mockBaseURL}/analytics/facets`);
    facetsReq.flush(mockFacetsResponse);
    tick();

    const updatedWidgets = component.dashboardWidgets();
    expect(updatedWidgets.length).toBe(2);
    expect(updatedWidgets[0].response).toEqual(mockMeasuresResponse);
    expect(updatedWidgets[1].response).toEqual(mockFacetsResponse);
  }));

  it('should trigger refresh when refreshToken input changes', fakeAsync(() => {
    const mockResponse: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          unit: 'NUMBER',
          measures: [{ name: 'COUNT', value: 100 }],
        },
      ],
    };

    const widgets: Widget[] = [
      {
        id: '1',
        title: 'Widget',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 0, y: 0 },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          metrics: [],
        },
      },
    ];

    fixture.componentRef.setInput('dashboard', buildDashboard(widgets));
    fixture.detectChanges();
    tick();

    const req1 = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    req1.flush(mockResponse);
    tick();

    fixture.componentRef.setInput('refreshToken', 1);
    fixture.detectChanges();
    tick();

    const req2 = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    expect(req2.request.method).toBe('POST');
    req2.flush(mockResponse);
    tick();
  }));

  it('should apply requestFilters to widget requests', fakeAsync(() => {
    const externalFilters: RequestFilter[] = [{ name: 'API', operator: 'IN', value: ['api-1', 'api-2'] }];

    const widgets: Widget[] = [
      {
        id: '1',
        title: 'Widget',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 0, y: 0 },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          metrics: [],
        },
      },
    ];

    fixture.componentRef.setInput('requestFilters', externalFilters);
    fixture.componentRef.setInput('dashboard', buildDashboard(widgets));
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    expect(req.request.method).toBe('POST');
    const requestBody = req.request.body as MeasuresRequest;
    expect(requestBody.filters).toBeDefined();
    expect(requestBody.filters!.length).toBeGreaterThan(0);
    const apiFilter = requestBody.filters!.find((f: RequestFilter) => f.name === 'API');
    expect(apiFilter).toBeDefined();
    expect(apiFilter!.value).toEqual(['api-1', 'api-2']);

    req.flush({
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          unit: 'NUMBER',
          measures: [{ name: 'COUNT', value: 100 }],
        },
      ],
    });
    tick();
  }));

  it('should merge requestFilters with widget-level filters at request time', fakeAsync(() => {
    const widgetLevelFilter: RequestFilter = { name: 'HTTP_STATUS', operator: 'IN', value: ['2xx'] };
    const externalFilters: RequestFilter[] = [{ name: 'API', operator: 'IN', value: ['api-1'] }];

    const widgets: Widget[] = [
      {
        id: '1',
        title: 'Widget with own filter',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 0, y: 0 },
        request: {
          type: 'measures',
          metrics: [],
          filters: [widgetLevelFilter],
        },
      },
    ];

    fixture.componentRef.setInput('requestFilters', externalFilters);
    fixture.componentRef.setInput('dashboard', buildDashboard(widgets));
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    const requestBody = req.request.body as MeasuresRequest;

    expect(requestBody.filters).toBeDefined();
    expect(requestBody.filters!.length).toBe(2);

    const httpStatusFilter = requestBody.filters!.find((f: RequestFilter) => f.name === 'HTTP_STATUS');
    expect(httpStatusFilter).toBeDefined();
    expect(httpStatusFilter!.value).toEqual(['2xx']);

    const apiFilter = requestBody.filters!.find((f: RequestFilter) => f.name === 'API');
    expect(apiFilter).toBeDefined();
    expect(apiFilter!.value).toEqual(['api-1']);

    expect(widgets[0].request!.filters).toEqual([widgetLevelFilter]);

    req.flush({
      metrics: [{ name: 'HTTP_REQUESTS', unit: 'NUMBER', measures: [{ name: 'COUNT', value: 50 }] }],
    });
    tick();
  }));
});
