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
import { AddFilterDialogComponent, Dashboard, GraviteeDashboardService } from '@gravitee/gravitee-dashboard';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { By } from '@angular/platform-browser';
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

const MOCK_DASHBOARD: Dashboard = {
  id: 'test-dashboard',
  name: 'Test Dashboard',
  createdBy: 'user',
  createdAt: new Date().toISOString(),
  lastModified: new Date().toISOString(),
  labels: {},
  widgets: [],
};

describe('DashboardViewerComponent', () => {
  let component: DashboardViewerComponent;
  let fixture: ComponentFixture<DashboardViewerComponent>;
  const mockGraviteeDashboardService = {
    getWidgets: jest.fn().mockReturnValue([]),
    getMetrics: jest.fn().mockReturnValue(of({ metrics: [] })),
  };

  const matDialogMock = {
    open: jest.fn().mockReturnValue({
      afterClosed: () => of(undefined),
    }),
  };

  beforeEach(async () => {
    matDialogMock.open.mockReturnValue({
      afterClosed: () => of(undefined),
    });
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
          provide: MatDialog,
          useValue: matDialogMock,
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
    fixture.componentRef.setInput('dashboard', MOCK_DASHBOARD);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should open add filter dialog when Add filter is clicked', () => {
    const addBtn = fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__add-btn'));
    expect(addBtn).toBeTruthy();
    addBtn.nativeElement.click();
    expect(matDialogMock.open).toHaveBeenCalledWith(
      AddFilterDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({
          timeFrom: expect.any(Number),
          timeTo: expect.any(Number),
        }),
        injector: expect.any(Object),
      }),
    );
  });

  it('should add condition to store when dialog closes with a value', () => {
    const condition = { field: 'API', label: 'API', operator: 'EQ' as const, values: ['id-1'] };
    matDialogMock.open.mockReturnValue({
      afterClosed: () => of(condition),
    });
    const addBtn = fixture.debugElement.query(By.css('.gd-dynamic-filter-bar__add-btn'));
    addBtn.nativeElement.click();
    expect(component.filtersStore.conditions()).toEqual([condition]);
  });
});
