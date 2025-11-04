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
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { MeasureName } from './components/widget/model/request/enum/measure-name';
import { MetricName } from './components/widget/model/request/enum/metric-name';
import { MeasuresResponse } from './components/widget/model/response/measures-response';
import { Widget } from './components/widget/model/widget/widget';
import { GraviteeDashboardComponent } from './gravitee-dashboard.component';

describe('GraviteeDashboardComponent', () => {
  let component: GraviteeDashboardComponent;
  let fixture: ComponentFixture<GraviteeDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(GraviteeDashboardComponent);
    component = fixture.componentInstance;

    const mockWidgets: Widget[] = [];
    component.widgets.set(mockWidgets);
    fixture.detectChanges();
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

    component.widgets.set(widgets);
    fixture.detectChanges();

    expect(component.widgets().length).toBe(1);
    expect(component.widgets()[0].response).toBeUndefined();
  });

  it('should not load data for widgets with existing response', () => {
    const mockResponse: MeasuresResponse = {
      type: 'measures',
      metrics: [
        {
          name: MetricName.HTTP_REQUESTS,
          measures: [{ name: MeasureName.COUNT, value: 100 }],
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

    component.widgets.set(widgets);
    fixture.detectChanges();

    expect(component.widgets()[0].response).toBeDefined();
    expect(component.widgets()[0].response).toEqual(mockResponse);
  });

  it('should load data for widgets with request but no response', waitForAsync(() => {
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

    component.widgets.set(widgets);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const updatedWidgets = component.widgets();
      expect(updatedWidgets.length).toBe(1);
      expect(updatedWidgets[0].response).toBeDefined();
      expect(updatedWidgets[0].response?.type).toBe('measures');
    });
  }));
});
