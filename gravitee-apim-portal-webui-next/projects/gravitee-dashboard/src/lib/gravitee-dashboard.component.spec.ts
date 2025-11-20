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

import { Filter } from './components/filter/generic-filter-bar/generic-filter-bar.component';
import { MeasuresResponse } from './components/widget/model/response/measures-response';
import { Widget } from './components/widget/model/widget/widget';
import { GraviteeDashboardComponent } from './gravitee-dashboard.component';

describe('GraviteeDashboardComponent', () => {
  let component: GraviteeDashboardComponent;
  let fixture: ComponentFixture<GraviteeDashboardComponent>;
  let httpTestingController: HttpTestingController;
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

    const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    fixture.whenStable().then(() => {
      const updatedWidgets = component.dashboardWidgets();
      expect(updatedWidgets.length).toBe(1);
      expect(updatedWidgets[0].response).toBeDefined();
      expect(updatedWidgets[0].response).toEqual(mockResponse);
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

    const req = httpTestingController.expectOne(`${mockBaseURL}/analytics/measures`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    fixture.whenStable().then(() => {
      const updatedWidgets = component.dashboardWidgets();
      expect(updatedWidgets.length).toBe(1);
      expect(updatedWidgets[0].response).toBeDefined();
      expect(updatedWidgets[0].response).toEqual(mockResponse);
    });
  }));
});
