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

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryChartComponent } from './category-chart.component';
import { FacetsResponse } from '../../widget/model/response/facets-response';
import { CategoryType } from '../../widget/model/widget/widget.model';
import { CHART_COLORS } from '../shared/chart-colors';

describe('CategoryChartComponent', () => {
  let component: CategoryChartComponent;
  let fixture: ComponentFixture<CategoryChartComponent>;

  const createFacetsResponse = (): FacetsResponse => ({
    metrics: [
      {
        name: 'HTTP_REQUESTS',
        unit: 'NUMBER',
        buckets: [
          { key: 'tools/call', name: 'tools/call', measures: [{ name: 'COUNT', value: 17 }] },
          { key: 'tools/list', name: 'tools/list', measures: [{ name: 'COUNT', value: 14 }] },
          { key: 'resources/list', name: 'resources/list', measures: [{ name: 'COUNT', value: 10 }] },
        ],
      },
    ],
  });

  const setChartData = (data: FacetsResponse, type: CategoryType = 'vertical-bar') => {
    fixture.componentRef.setInput('type', type);
    fixture.componentRef.setInput('data', data);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryChartComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    setChartData(createFacetsResponse());
    expect(component).toBeTruthy();
  });

  it('should convert facets data to chart data with labels', () => {
    setChartData(createFacetsResponse());

    const result = component.dataFormatted();
    expect(result.labels).toEqual(['tools/call', 'tools/list', 'resources/list']);
    expect(result.datasets[0].data).toEqual([17, 14, 10]);
  });

  it('should assign a different color per bar', () => {
    setChartData(createFacetsResponse());

    const result = component.dataFormatted();
    const bgColors = result.datasets[0].backgroundColor as string[];
    expect(bgColors[0]).toBe(CHART_COLORS[0]);
    expect(bgColors[1]).toBe(CHART_COLORS[1]);
    expect(bgColors[2]).toBe(CHART_COLORS[2]);
  });

  it('should use vertical indexAxis by default', () => {
    setChartData(createFacetsResponse(), 'vertical-bar');

    const options = component.chartOptions();
    expect(options?.indexAxis).toBe('x');
  });

  it('should use horizontal indexAxis for horizontal-bar', () => {
    setChartData(createFacetsResponse(), 'horizontal-bar');

    const options = component.chartOptions();
    expect(options?.indexAxis).toBe('y');
  });

  it('should handle empty metrics', () => {
    setChartData({ metrics: [] });

    const result = component.dataFormatted();
    expect(result.labels).toEqual([]);
  });
});
