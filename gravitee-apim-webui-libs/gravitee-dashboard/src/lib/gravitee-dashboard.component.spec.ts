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
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { Filter, SelectedFilter } from './components/filter/generic-filter-bar/generic-filter-bar.component';
import { MeasuresRequest } from './components/widget/model/request/measures-request';
import { RequestFilter } from './components/widget/model/request/request';
import { FacetsResponse } from './components/widget/model/response/facets-response';
import { MeasuresResponse } from './components/widget/model/response/measures-response';
import { Widget } from './components/widget/model/widget/widget.model';
import { GraviteeDashboardComponent } from './gravitee-dashboard.component';

describe('GraviteeDashboardComponent', () => {
  let component: GraviteeDashboardComponent;
  let fixture: ComponentFixture<GraviteeDashboardComponent>;
  let httpTestingController: HttpTestingController;
  let router: Router;
  let activatedRoute: ActivatedRoute;
  const mockBaseURL = 'http://customURL';
  const mockFilters: Filter[] = [];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({}),
            snapshot: { params: {}, queryParams: {} },
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: jest.fn().mockResolvedValue(true),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GraviteeDashboardComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    activatedRoute = TestBed.inject(ActivatedRoute);

    fixture.componentRef.setInput('baseURL', mockBaseURL);
    fixture.componentRef.setInput('filters', mockFilters);
    fixture.componentRef.setInput('widgetConfigs', []);
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

    fixture.componentRef.setInput('widgetConfigs', widgets);
    fixture.detectChanges();

    const dashboardWidgets = component.dashboardWidgets();
    expect(dashboardWidgets.length).toBe(1);
    expect(dashboardWidgets[0].response).toBeUndefined();
  });

  it('should reload data for widgets even if they have existing response', waitForAsync(() => {
    const mockResponse: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
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

    fixture.componentRef.setInput('widgetConfigs', widgets);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);

      fixture.whenStable().then(() => {
        const updatedWidgets = component.dashboardWidgets();
        expect(updatedWidgets.length).toBe(1);
        expect(updatedWidgets[0].response).toBeDefined();
        expect(updatedWidgets[0].response).toEqual(mockResponse);
      });
    });
  }));

  it('should load data for widgets with request but no response', waitForAsync(() => {
    const mockResponse: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
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
    fixture.componentRef.setInput('widgetConfigs', widgets);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);

      fixture.whenStable().then(() => {
        const updatedWidgets = component.dashboardWidgets();
        expect(updatedWidgets.length).toBe(1);
        expect(updatedWidgets[0].response).toBeDefined();
        expect(updatedWidgets[0].response).toEqual(mockResponse);
      });
    });
  }));

  it('should load data for widgets with facets request', waitForAsync(() => {
    const mockResponse: FacetsResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
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

    fixture.componentRef.setInput('widgetConfigs', widgets);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/facets`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);

      fixture.whenStable().then(() => {
        const updatedWidgets = component.dashboardWidgets();
        expect(updatedWidgets.length).toBe(1);
        expect(updatedWidgets[0].response).toBeDefined();
        expect(updatedWidgets[0].response).toEqual(mockResponse);
      });
    });
  }));

  it('should handle multiple widgets with different request types', waitForAsync(() => {
    const mockMeasuresResponse: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          measures: [{ name: 'COUNT', value: 100 }],
        },
      ],
    };

    const mockFacetsResponse: FacetsResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
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

    fixture.componentRef.setInput('widgetConfigs', widgets);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const measuresReq = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
      measuresReq.flush(mockMeasuresResponse);

      const facetsReq = httpTestingController.expectOne(`${mockBaseURL}/analytics/facets`);
      facetsReq.flush(mockFacetsResponse);

      fixture.whenStable().then(() => {
        const updatedWidgets = component.dashboardWidgets();
        expect(updatedWidgets.length).toBe(2);
        expect(updatedWidgets[0].response).toEqual(mockMeasuresResponse);
        expect(updatedWidgets[1].response).toEqual(mockFacetsResponse);
      });
    });
  }));

  it('should navigate with query params when filters are selected', () => {
    const selectedFilters: SelectedFilter[] = [
      { parentKey: 'period', value: '1d' },
      { parentKey: 'API', value: 'api-1' },
      { parentKey: 'API', value: 'api-2' },
    ];

    component.onSelectedFilters(selectedFilters);

    expect(router.navigate).toHaveBeenCalledWith(['.'], {
      relativeTo: activatedRoute,
      queryParams: {
        period: '1d',
        API: 'api-1,api-2',
      },
    });
  });

  it('should navigate with custom period and from/to params', () => {
    const selectedFilters: SelectedFilter[] = [
      { parentKey: 'period', value: 'custom' },
      { parentKey: 'from', value: '1704067200000' },
      { parentKey: 'to', value: '1704153600000' },
      { parentKey: 'API', value: 'api-1' },
    ];

    component.onSelectedFilters(selectedFilters);

    expect(router.navigate).toHaveBeenCalledWith(['.'], {
      relativeTo: activatedRoute,
      queryParams: {
        period: 'custom',
        from: '1704067200000',
        to: '1704153600000',
        API: 'api-1',
      },
    });
  });

  it('should trigger refresh when onRefresh is called', waitForAsync(() => {
    const mockResponse: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
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

    fixture.componentRef.setInput('widgetConfigs', widgets);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const req1 = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
      req1.flush(mockResponse);

      fixture.whenStable().then(() => {
        component.onRefresh();
        fixture.detectChanges();

        fixture.whenStable().then(() => {
          const req2 = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
          expect(req2.request.method).toBe('POST');
          req2.flush(mockResponse);
        });
      });
    });
  }));

  it('should parse query params and initialize selected filters', () => {
    const queryParams = {
      period: '1d',
      from: '1704067200000',
      to: '1704153600000',
      API: 'api-1,api-2',
      APPLICATION: 'app-1',
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of(queryParams),
            snapshot: { params: {}, queryParams },
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: jest.fn().mockResolvedValue(true),
          },
        },
      ],
    });

    const newFixture = TestBed.createComponent(GraviteeDashboardComponent);
    newFixture.componentRef.setInput('baseURL', mockBaseURL);
    newFixture.componentRef.setInput('filters', mockFilters);
    newFixture.componentRef.setInput('widgetConfigs', []);
    newFixture.detectChanges();

    const component = newFixture.componentInstance;
    const selectedFilters = component.currentSelectedFilters();

    expect(selectedFilters.length).toBe(6);
    expect(selectedFilters.find(f => f.parentKey === 'period')?.value).toBe('1d');
    expect(selectedFilters.find(f => f.parentKey === 'from')?.value).toBe('1704067200000');
    expect(selectedFilters.find(f => f.parentKey === 'to')?.value).toBe('1704153600000');
    expect(selectedFilters.filter(f => f.parentKey === 'API').length).toBe(2);
    expect(selectedFilters.filter(f => f.parentKey === 'APPLICATION').length).toBe(1);
  });

  it('should handle empty query params', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({}),
            snapshot: { params: {}, queryParams: {} },
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: jest.fn().mockResolvedValue(true),
          },
        },
      ],
    });

    const newFixture = TestBed.createComponent(GraviteeDashboardComponent);
    newFixture.componentRef.setInput('baseURL', mockBaseURL);
    newFixture.componentRef.setInput('filters', mockFilters);
    newFixture.componentRef.setInput('widgetConfigs', []);
    newFixture.detectChanges();

    const component = newFixture.componentInstance;
    const selectedFilters = component.currentSelectedFilters();

    expect(selectedFilters.length).toBe(0);
  });

  it('should apply filters to widget requests', waitForAsync(() => {
    const queryParams = {
      period: '1d',
      API: 'api-1,api-2',
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of(queryParams),
            snapshot: { params: {}, queryParams },
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: jest.fn().mockResolvedValue(true),
          },
        },
      ],
    });

    const newFixture = TestBed.createComponent(GraviteeDashboardComponent);
    const newHttpTestingController = TestBed.inject(HttpTestingController);
    newFixture.componentRef.setInput('baseURL', mockBaseURL);
    newFixture.componentRef.setInput('filters', mockFilters);
    newFixture.componentRef.setInput('widgetConfigs', [
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
    ]);
    newFixture.detectChanges();

    newFixture.whenStable().then(() => {
      const req = newHttpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
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
            measures: [{ name: 'COUNT', value: 100 }],
          },
        ],
      });
    });
  }));

  it('should handle null or undefined query param values', () => {
    const queryParams = {
      period: '1d',
      API: null,
      APPLICATION: undefined,
      EMPTY: '',
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of(queryParams),
            snapshot: { params: {}, queryParams },
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: jest.fn().mockResolvedValue(true),
          },
        },
      ],
    });

    const newFixture = TestBed.createComponent(GraviteeDashboardComponent);
    newFixture.componentRef.setInput('baseURL', mockBaseURL);
    newFixture.componentRef.setInput('filters', mockFilters);
    newFixture.componentRef.setInput('widgetConfigs', []);
    newFixture.detectChanges();

    const component = newFixture.componentInstance;
    const selectedFilters = component.currentSelectedFilters();

    expect(selectedFilters.find(f => f.parentKey === 'API')).toBeUndefined();
    expect(selectedFilters.find(f => f.parentKey === 'APPLICATION')).toBeUndefined();
    expect(selectedFilters.find(f => f.parentKey === 'period')?.value).toBe('1d');
  });

  it('should handle array query param values', () => {
    const queryParams = {
      period: '1d',
      API: ['api-1', 'api-2'],
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of(queryParams),
            snapshot: { params: {}, queryParams },
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: jest.fn().mockResolvedValue(true),
          },
        },
      ],
    });

    const newFixture = TestBed.createComponent(GraviteeDashboardComponent);
    newFixture.componentRef.setInput('baseURL', mockBaseURL);
    newFixture.componentRef.setInput('filters', mockFilters);
    newFixture.componentRef.setInput('widgetConfigs', []);
    newFixture.detectChanges();

    const component = newFixture.componentInstance;
    const selectedFilters = component.currentSelectedFilters();

    expect(selectedFilters.filter(f => f.parentKey === 'API').length).toBe(2);
    expect(selectedFilters.find(f => f.parentKey === 'API' && f.value === 'api-1')).toBeDefined();
    expect(selectedFilters.find(f => f.parentKey === 'API' && f.value === 'api-2')).toBeDefined();
  });
});
