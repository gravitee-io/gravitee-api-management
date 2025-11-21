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
import { GraviteeDashboardService } from '@gravitee/gravitee-dashboard';

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AnalyticsViewerComponent } from './analytics-viewer.component';

import { CONSTANTS_TESTING } from '../../../shared/testing';
import { Constants } from '../../../entities/Constants';

// Mock ResizeObserver to avoid errors in tests using canvas (Chartjs)
globalThis.ResizeObserver =
  globalThis.ResizeObserver ||
  jest.fn().mockImplementation(() => ({
    disconnect: jest.fn(),
    observe: jest.fn(),
    unobserve: jest.fn(),
  }));

describe('AnalyticsViewerComponent', () => {
  let component: AnalyticsViewerComponent;
  let fixture: ComponentFixture<AnalyticsViewerComponent>;
  const mockGraviteeDashboardService = {
    getWidgets: jest.fn().mockReturnValue([]),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsViewerComponent],
      providers: [
        {
          provide: GraviteeDashboardService,
          useValue: mockGraviteeDashboardService,
        },
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
