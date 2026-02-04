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
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { DashboardViewerComponent } from './dashboard-viewer.component';

import { Constants } from '../../../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';

// Mock ResizeObserver to avoid errors in tests using canvas (Chartjs)
globalThis.ResizeObserver =
  globalThis.ResizeObserver ||
  jest.fn().mockImplementation(() => ({
    disconnect: jest.fn(),
    observe: jest.fn(),
    unobserve: jest.fn(),
  }));

describe('DashboardViewerComponent', () => {
  let component: DashboardViewerComponent;
  let fixture: ComponentFixture<DashboardViewerComponent>;
  const mockGraviteeDashboardService = {
    getWidgets: jest.fn().mockReturnValue([]),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardViewerComponent, GioTestingModule],
      providers: [
        {
          provide: GraviteeDashboardService,
          useValue: mockGraviteeDashboardService,
        },
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
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
            initialNavigation: jest.fn(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
