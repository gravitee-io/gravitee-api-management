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

import { StatsComponent } from './stats.component';
import { MeasuresResponse } from '../../widget/model/response/measures-response';

describe('StatsComponent', () => {
  let component: StatsComponent;
  let fixture: ComponentFixture<StatsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StatsComponent);
    component = fixture.componentInstance;

    const mockData: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          measures: [
            { name: 'COUNT', value: 10 },
            { name: 'AVG', value: 25 },
          ],
        },
      ],
    };

    component.data.set(mockData);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should handle empty metrics list', () => {
    const mockData: MeasuresResponse = {
      metrics: [],
    };

    component.data.set(mockData);
    fixture.detectChanges();

    expect(component.dataFormatted()).toEqual([]);
  });

  it('should handle a single measure', () => {
    const mockData: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          measures: [{ name: 'COUNT', value: 42 }],
        },
      ],
    };

    component.data.set(mockData);
    fixture.detectChanges();

    expect(component.dataFormatted()).toEqual(['42']);
  });

  it('should handle two measures', () => {
    const mockData: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          measures: [
            { name: 'COUNT', value: 15 },
            { name: 'AVG', value: 30 },
          ],
        },
      ],
    };

    component.data.set(mockData);
    fixture.detectChanges();

    expect(component.dataFormatted()).toEqual(['15', '30 ms']);
  });
});
