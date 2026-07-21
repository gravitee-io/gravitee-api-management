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
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';

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

  const permissionMock = {
    hasAnyMatching: jest.fn().mockReturnValue(true),
  };

  beforeEach(async () => {
    matDialogMock.open.mockReturnValue({
      afterClosed: () => of(undefined),
    });
    permissionMock.hasAnyMatching.mockReturnValue(true);

    await TestBed.configureTestingModule({
      imports: [DashboardViewerComponent, GioTestingModule],
      providers: [
        { provide: GraviteeDashboardService, useValue: mockGraviteeDashboardService },
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: MatDialog, useValue: matDialogMock },
        { provide: GioPermissionService, useValue: permissionMock },
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

  it('should render the timeframe selector row', () => {
    const timeRow = fixture.debugElement.query(By.css('.observability-dashboard-viewer__time-row'));
    expect(timeRow).toBeTruthy();
    const timeframeSelector = timeRow.query(By.css('gd-timeframe-selector'));
    expect(timeframeSelector).toBeTruthy();
  });

  it('should render the filters row', () => {
    const filtersRow = fixture.debugElement.query(By.css('.observability-dashboard-viewer__filters-row'));
    expect(filtersRow).toBeTruthy();
    const filterBar = filtersRow.query(By.css('gd-dynamic-filter-bar'));
    expect(filterBar).toBeTruthy();
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

  // Filters are transient query state — DashboardFiltersStore never persists them — so every filter
  // interaction must stay available without environment-dashboard-u (APIM-14729).
  //
  // Two distinct guards are at work here, and they are not equally strong:
  //  - the permission stub only catches re-introduction of the removed
  //    hasAnyMatching(['environment-dashboard-u']) gate; the component no longer injects
  //    GioPermissionService, so on its own it asserts nothing about the component;
  //  - the DOM-driven assertions are what catch a re-gate by any other mechanism, since add / edit /
  //    remove / clear are all rendered off gd-dynamic-filter-bar's `editable` input. Anything that
  //    makes that input falsy fails all four tests below.
  describe('when the user has no dashboard permission', () => {
    const API_CONDITION = { field: 'API', label: 'API', operator: 'EQ' as const, values: ['id-1'] };
    const APPLICATION_CONDITION = { field: 'APPLICATION', label: 'Application', operator: 'EQ' as const, values: ['app-1'] };

    let fixtureWithoutPermission: ComponentFixture<DashboardViewerComponent>;
    let viewerWithoutPermission: DashboardViewerComponent;

    beforeEach(() => {
      matDialogMock.open.mockClear();
      permissionMock.hasAnyMatching.mockReturnValue(false);
      fixtureWithoutPermission = TestBed.createComponent(DashboardViewerComponent);
      viewerWithoutPermission = fixtureWithoutPermission.componentInstance;
      fixtureWithoutPermission.componentRef.setInput('dashboard', MOCK_DASHBOARD);
      fixtureWithoutPermission.detectChanges();
    });

    function seedConditions(...conditions: (typeof API_CONDITION)[]): void {
      conditions.forEach(condition => viewerWithoutPermission.filtersStore.add(condition));
      fixtureWithoutPermission.detectChanges();
    }

    it('should allow adding a filter', () => {
      matDialogMock.open.mockReturnValue({ afterClosed: () => of(API_CONDITION) });

      const addBtn = fixtureWithoutPermission.debugElement.query(By.css('.gd-dynamic-filter-bar__add-btn'));
      expect(addBtn).toBeTruthy();
      addBtn.nativeElement.click();

      expect(viewerWithoutPermission.filtersStore.conditions()).toEqual([API_CONDITION]);
    });

    it('should allow editing a filter', () => {
      seedConditions(API_CONDITION);
      const editedCondition = { ...API_CONDITION, values: ['id-2'] };
      matDialogMock.open.mockReturnValue({ afterClosed: () => of(editedCondition) });

      const chip = fixtureWithoutPermission.debugElement.query(By.css('gd-filter-chip mat-chip'));
      expect(chip).toBeTruthy();
      chip.nativeElement.click();

      expect(matDialogMock.open).toHaveBeenCalledWith(
        AddFilterDialogComponent,
        expect.objectContaining({
          data: expect.objectContaining({ existingCondition: API_CONDITION }),
        }),
      );
      expect(viewerWithoutPermission.filtersStore.conditions()).toEqual([editedCondition]);
    });

    it('should allow removing a single filter', () => {
      seedConditions(API_CONDITION, APPLICATION_CONDITION);

      const removeIcon = fixtureWithoutPermission.debugElement.query(By.css('gd-filter-chip .mat-mdc-chip-remove'));
      expect(removeIcon).toBeTruthy();
      removeIcon.nativeElement.click();

      expect(viewerWithoutPermission.filtersStore.conditions()).toEqual([APPLICATION_CONDITION]);
    });

    it('should allow clearing all filters', () => {
      seedConditions(API_CONDITION, APPLICATION_CONDITION);

      const clearBtn = fixtureWithoutPermission.debugElement.query(By.css('.gd-dynamic-filter-bar__clear-btn'));
      expect(clearBtn).toBeTruthy();
      clearBtn.nativeElement.click();

      expect(viewerWithoutPermission.filtersStore.conditions()).toEqual([]);
    });
  });
});
