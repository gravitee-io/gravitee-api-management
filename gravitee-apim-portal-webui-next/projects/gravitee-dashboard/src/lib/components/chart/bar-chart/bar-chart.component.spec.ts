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

import { BarChartComponent } from './bar-chart.component';
import { TimeSeriesBucket, TimeSeriesResponse } from '../../widget/model/response/time-series-response';
import { CHART_COLORS } from '../shared/chart-colors';

describe('BarChartComponent', () => {
  let component: BarChartComponent;
  let fixture: ComponentFixture<BarChartComponent>;

  const createBaseBucket = (key: string, overrides: Partial<TimeSeriesBucket> = {}): TimeSeriesBucket => ({
    key,
    name: key,
    timestamp: new Date(key),
    ...overrides,
  });

  const createMeasureBucket = (key: string, value: number): TimeSeriesBucket =>
    createBaseBucket(key, {
      measures: [
        {
          name: 'COUNT',
          value,
        },
      ],
    });

  const createNestedBucket = (key: string, groups: Record<string, number>): TimeSeriesBucket =>
    createBaseBucket(key, {
      buckets: Object.entries(groups).map(([range, value]) => ({
        key: range,
        name: range,
        measures: [
          {
            name: 'COUNT',
            value,
          },
        ],
      })),
    });

  const createTimeSeriesResponse = (metricBuckets: TimeSeriesBucket[]): TimeSeriesResponse => ({
    metrics: [
      {
        name: 'HTTP_REQUESTS',
        buckets: metricBuckets,
      },
    ],
    buckets: metricBuckets.map(({ key, name, timestamp }) => createBaseBucket(key, { name, timestamp })),
  });

  const setChartData = (data: TimeSeriesResponse) => {
    fixture.componentRef.setInput('data', data);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BarChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BarChartComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    const data = createTimeSeriesResponse([createMeasureBucket('2025-10-07T06:00:00Z', 100)]);

    setChartData(data);

    expect(component).toBeTruthy();
  });

  it('should handle multiple buckets', () => {
    const data = createTimeSeriesResponse([
      createMeasureBucket('2025-10-07T06:00:00Z', 100),
      createMeasureBucket('2025-10-08T06:00:00Z', 200),
      createMeasureBucket('2025-10-09T06:00:00Z', 150),
    ]);

    setChartData(data);

    const result = component.dataFormatted();
    expect(result.labels?.length).toBe(3);
    expect(result.datasets[0].data).toEqual([100, 200, 150]);
  });

  it('should handle empty metrics list', () => {
    const emptyData: TimeSeriesResponse = {
      metrics: [],
      buckets: [],
    };

    setChartData(emptyData);

    const result = component.dataFormatted();
    expect(result.labels).toEqual([]);
    expect(result.datasets).toEqual([]);
  });

  it('should handle nested buckets structure', () => {
    const data = createTimeSeriesResponse([
      createNestedBucket('2025-10-07T06:00:00Z', {
        '100-199': 50,
        '200-299': 30,
      }),
      createNestedBucket('2025-10-08T06:00:00Z', {
        '100-199': 60,
        '200-299': 40,
      }),
    ]);

    setChartData(data);

    const result = component.dataFormatted();
    expect(result.labels?.length).toBe(2);
    expect(result.datasets.length).toBe(2);
    expect(result.datasets[0].label).toContain('100-199');
    expect(result.datasets[1].label).toContain('200-299');
  });

  it('should have stacked bar chart options by default', () => {
    const data = createTimeSeriesResponse([createMeasureBucket('2025-10-07T06:00:00Z', 100)]);
    setChartData(data);

    const options = component.option();
    const xScale = options?.scales?.['x'];
    const yScale = options?.scales?.['y'];

    expect(xScale?.stacked).toBe(true);
    expect(yScale?.stacked).toBe(true);
    expect((yScale as { beginAtZero?: boolean })?.beginAtZero).toBe(true);
  });

  it('should assign shared colors to datasets', () => {
    const data: TimeSeriesResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          buckets: [createMeasureBucket('2025-10-07T06:00:00Z', 100)],
        },
        {
          name: 'HTTP_ERRORS',
          buckets: [createMeasureBucket('2025-10-07T06:00:00Z', 200)],
        },
      ],
      buckets: [createBaseBucket('2025-10-07T06:00:00Z')],
    };

    setChartData(data);

    const result = component.dataFormatted();
    expect(result.datasets[0].backgroundColor).toBe(CHART_COLORS[0]);
    expect(result.datasets[1].backgroundColor).toBe(CHART_COLORS[1]);
  });
});
