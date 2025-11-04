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

import { PieChartComponent } from './pie-chart.component';
import { MeasureName } from '../../widget/model/request/enum/measure-name';
import { MetricName } from '../../widget/model/request/enum/metric-name';
import { FacetsResponse } from '../../widget/model/response/facets-response';

describe('PieChartComponent', () => {
  let component: PieChartComponent;
  let fixture: ComponentFixture<PieChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PieChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PieChartComponent);
    component = fixture.componentInstance;

    const mockData: FacetsResponse = {
      type: 'facets',
      metrics: [
        {
          name: MetricName.HTTP_REQUESTS,
          buckets: [
            {
              key: 'test-key-1',
              measures: [
                {
                  name: MeasureName.COUNT,
                  value: 100,
                },
              ],
            },
          ],
        },
      ],
    };

    fixture.componentRef.setInput('type', 'pie');
    fixture.componentRef.setInput('data', mockData);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should handle multiple buckets', () => {
    const mockData: FacetsResponse = {
      type: 'facets',
      metrics: [
        {
          name: MetricName.HTTP_REQUESTS,
          buckets: [
            {
              key: 'bucket-1',
              measures: [
                {
                  name: MeasureName.COUNT,
                  value: 100,
                },
              ],
            },
            {
              key: 'bucket-2',
              measures: [
                {
                  name: MeasureName.COUNT,
                  value: 200,
                },
              ],
            },
            {
              key: 'bucket-3',
              measures: [
                {
                  name: MeasureName.COUNT,
                  value: 150,
                },
              ],
            },
          ],
        },
      ],
    };

    fixture.componentRef.setInput('data', mockData);
    fixture.detectChanges();

    const result = component.dataFormatted();
    expect(result.labels).toEqual(['bucket-1', 'bucket-2', 'bucket-3']);
    expect(result.datasets[0].data).toEqual([100, 200, 150]);
  });

  it('should handle empty metrics list', () => {
    const mockData: FacetsResponse = {
      type: 'facets',
      metrics: [],
    };

    fixture.componentRef.setInput('data', mockData);
    fixture.detectChanges();

    const result = component.dataFormatted();
    expect(result.labels).toEqual([]);
    expect(result.datasets[0].data).toEqual([]);
  });
});
