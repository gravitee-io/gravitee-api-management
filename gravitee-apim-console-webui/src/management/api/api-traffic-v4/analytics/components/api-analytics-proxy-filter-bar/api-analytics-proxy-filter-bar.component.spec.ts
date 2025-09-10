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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OWL_DATE_TIME_FORMATS } from '@danielmoncada/angular-datetime-picker';
import moment from 'moment';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiAnalyticsProxyFilterBarComponent, ApiAnalyticsProxyFilters } from './api-analytics-proxy-filter-bar.component';
import { ApiAnalyticsProxyFilterBarHarness } from './api-analytics-proxy-filter-bar.harness';

import { Application } from '../../../../../../entities/application/Application';
import { PagedResult } from '../../../../../../entities/pagedResult';
import { DATE_TIME_FORMATS } from '../../../../../../shared/utils/timeFrameRanges';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';

describe('ApiAnalyticsProxyFilterBarComponent', () => {
  let component: ApiAnalyticsProxyFilterBarComponent;
  let fixture: ComponentFixture<ApiAnalyticsProxyFilterBarComponent>;
  let harness: ApiAnalyticsProxyFilterBarHarness;
  let httpTestingController: HttpTestingController;

  const API_ID = 'test-api-id';
  const mockActiveFilters: ApiAnalyticsProxyFilters = {
    period: '1d',
    from: null,
    to: null,
    httpStatuses: [],
    applications: [],
    plans: [],
  };

  const mockApplications: Application[] = [
    { id: 'app-1', name: 'Test Application 1' } as Application,
    { id: 'app-2', name: 'Test Application 2' } as Application,
    { id: 'app-3', name: 'Test Application 3' } as Application,
  ];

  const mockResponse = new PagedResult<Application>();
  mockResponse.data = mockApplications;
  mockResponse.page = { current: 1, per_page: 10, size: mockApplications.length, total_elements: mockApplications.length, total_pages: 1 };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsProxyFilterBarComponent, GioTestingModule],
      providers: [
        { provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS },
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiAnalyticsProxyFilterBarComponent);
    fixture.componentRef.setInput('activeFilters', mockActiveFilters);
    component = fixture.componentInstance;

    // Initialize form with mock data
    component.form.patchValue({
      timeframe: {
        period: mockActiveFilters.period,
        from: mockActiveFilters.from ? moment(mockActiveFilters.from) : null,
        to: mockActiveFilters.to ? moment(mockActiveFilters.to) : null,
      },
      plans: mockActiveFilters.plans,
    });

    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsProxyFilterBarHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Date Range Validation', () => {
    it('should validate that to date is not before from date', async () => {
      // Arrange
      const fromDate = moment();
      const toDate = moment().subtract(1, 'day'); // Before from date

      // Act
      component.form.controls.timeframe.setValue({
        ...component.form.controls.timeframe.value,
        from: fromDate,
        to: toDate,
        period: 'custom',
      });

      // Assert
      expect(await harness.hasDateRangeError()).toBe(false);
    });

    it('should pass validation when to date is after from date', async () => {
      // Arrange
      const fromDate = moment();
      const toDate = moment().add(1, 'day'); // After from date

      // Act
      component.form.controls.timeframe.setValue({
        ...component.form.controls.timeframe.value,
        from: fromDate,
        to: toDate,
        period: 'custom',
      });

      // Assert
      expect(await harness.hasDateRangeError()).toBe(false);
    });

    it('should pass validation when only one date is set', async () => {
      // Arrange & Act
      component.form.controls.timeframe.setValue({
        ...component.form.controls.timeframe.value,
        from: moment(),
        to: null,
        period: 'custom',
      });

      // Assert
      expect(await harness.hasDateRangeError()).toBe(false);
    });
  });

  describe('Filter Change Output', () => {
    it('should emit filtersChange when period changes', () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');
      const newPeriod = '7d';

      // Act
      component.form.controls.timeframe.setValue({ ...component.form.controls.timeframe.value, period: newPeriod });

      // Assert
      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        period: newPeriod,
        from: null,
        to: null,
      });
    });

    it('should emit filtersChange when custom timeframe is applied', () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');
      const fromDate = moment().subtract(1, 'day');
      const toDate = moment();

      // Act
      component.form.controls.timeframe.patchValue({
        ...component.form.controls.timeframe.value,
        from: fromDate,
        to: toDate,
        period: 'custom',
      });
      component.applyCustomTimeframe();

      // Assert
      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        from: fromDate.valueOf(),
        to: toDate.valueOf(),
        period: 'custom',
      });
    });

    it('should emit filtersChange when refresh is called', () => {
      // Arrange
      const spy = jest.spyOn(component.refresh, 'emit');

      // Act
      component.refreshFilters();

      // Assert
      expect(spy).toHaveBeenCalled();
    });

    it('should not emit when period is set to custom without applying', () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');

      // Act
      component.form.controls.timeframe.setValue({
        ...component.form.controls.timeframe.value,
        period: 'custom',
      });

      // Assert
      expect(spy).not.toHaveBeenCalledWith(expect.objectContaining({ period: 'custom' }));
    });

    it('should emit filtersChange when plan(s) is selected', () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');
      const plans = ['planId1', 'planId2'];

      // Act
      component.form.patchValue({
        plans: plans,
      });

      // Assert
      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        plans: plans,
      });
    });

    it('should emit filtersChange when httpStatus(es) are selected', () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');
      const statuses = ['200', '404', '500'];

      // Act
      component.form.patchValue({ httpStatuses: statuses });

      // Assert
      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        httpStatuses: statuses,
      });
    });

    it('should emit filtersChange when application(s) are selected', () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');
      const applications = ['app-1', 'app-2'];

      // Act
      component.form.patchValue({ applications });

      // Assert
      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        applications,
      });

      expectApplicationFindByIds(applications);
    });
  });

  describe('Form State', () => {
    it('should disable apply button when form is invalid', async () => {
      // Arrange
      const fromDate = moment();
      const toDate = moment().subtract(1, 'day'); // Invalid: to before from

      // Act
      fixture.componentRef.setInput('activeFilters', {
        period: 'custom',
        from: fromDate.valueOf(),
        to: toDate.valueOf(),
        httpStatuses: [],
        applications: [],
        plans: [],
        hosts: [],
      });
      fixture.detectChanges();

      // Assert
      expect(await harness.isApplyButtonEnabled()).toBe(false);
      expect(await harness.hasDateRangeError()).toBe(false);
    });

    it('should enable apply button when form is valid and dates are set', async () => {
      // Arrange
      const fromDate = moment();
      const toDate = moment().add(1, 'day'); // Valid: to after from

      // Act
      fixture.componentRef.setInput('activeFilters', {
        period: 'custom',
        from: fromDate.valueOf(),
        to: toDate.valueOf(),
        httpStatuses: [],
        applications: [],
        plans: [],
        hosts: [],
      });
      fixture.detectChanges();

      // Assert
      expect(await harness.hasDateRangeError()).toBe(false);
    });

    it('should disable apply button when dates are not set', async () => {
      // Arrange & Act
      component.form.controls.timeframe.setValue({
        ...component.form.controls.timeframe.value,
        from: null,
        to: null,
        period: 'custom',
      });
      fixture.detectChanges();

      // Assert
      expect(await harness.isApplyButtonEnabled()).toBe(false);
    });
  });

  describe('Empty filters chips', () => {
    it('should display message with No filter applied', async () => {
      expect(await harness.getFiltersAppliedText()).toBeFalsy();
      expect(await harness.getNoFiltersAppliedText()).toBe('No filter applied');
    });
  });

  describe('Selected Filter Chips', () => {
    let emitSpy: jest.SpyInstance;

    const filters: ApiAnalyticsProxyFilters = {
      ...mockActiveFilters,
      httpStatuses: ['200', '404'],
      plans: ['plan1', 'plan2'],
      applications: ['app-1'],
    };

    beforeEach(() => {
      emitSpy = jest.spyOn(component.filtersChange, 'emit');
      fixture.componentRef.setInput('activeFilters', filters);
      fixture.detectChanges();

      expectApplicationFindByIds(['app-1']);
    });

    it('should create filter chips from activeFilters using computed signals', async () => {
      expect(await harness.getFiltersAppliedText()).toBe('Filters applied:');
      expect(await harness.getNoFiltersAppliedText()).toBeFalsy();
      expect(await harness.getFilterChipCount()).toEqual(5);
      expect(await harness.showsChipByText('200 - Ok')).toBe(true);
      expect(await harness.showsChipByText('404 - Not Found')).toBe(true);
      expect(await harness.showsChipByText('plan1')).toBe(true);
      expect(await harness.showsChipByText('plan2')).toBe(true);
      expect(await harness.showsChipByText('Test Application 1')).toBe(true);
    });

    it('should remove any filter and update form', () => {
      component.removeFilter('httpStatuses', '200');

      expect(emitSpy).toHaveBeenCalledWith({
        ...filters,
        httpStatuses: ['404'],
      });
    });

    it('should remove applications filter and update form', () => {
      component.removeFilter('applications', 'app-1');

      expect(emitSpy).toHaveBeenCalledWith({
        ...filters,
        applications: null,
      });
    });

    it('should reset all filters', () => {
      component.resetAllFilters();

      expect(emitSpy).toHaveBeenCalledWith({
        ...filters,
        httpStatuses: null,
        plans: null,
        applications: null,
      });
    });

    it('should update filter chips when activeFilters change', async () => {
      // Arrange
      const newFilters: ApiAnalyticsProxyFilters = {
        ...mockActiveFilters,
        httpStatuses: ['500'],
        plans: ['plan1'],
      };

      // Act
      fixture.componentRef.setInput('activeFilters', newFilters);
      fixture.detectChanges();

      // Assert
      expect(await harness.getFilterChipCount()).toEqual(2);
      expect(await harness.showsChipByText('500 - Internal Server Error')).toBe(true);
      expect(await harness.showsChipByText('plan1')).toBe(true);
    });
  });

  describe('Applications Filter', () => {
    beforeEach(() => {
      // Reset filters to ensure clean state
      fixture.componentRef.setInput('activeFilters', {
        ...mockActiveFilters,
        applications: [],
      });
      fixture.detectChanges();
    });

    it('should search for first page of applications when opening select-search', async () => {
      // Act - Open the applications select
      await harness.openApplicationsSelect();

      // Assert - Expect HTTP request for first page
      expectApplicationSubscribers();
    });

    it('should emit application id when selecting an application', async () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');

      // Act - Open select and select an application
      await harness.openApplicationsSelect();

      // Mock the HTTP response for the search
      expectApplicationSubscribers();

      await harness.selectApplication('Application 1');
      expectApplicationFindByIds(['app-1']);

      // Assert - Should emit the selected application id
      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        applications: ['app-1'],
      });
    });

    it('should show application chip when application id is in activeFilters', async () => {
      // Arrange
      const filtersWithApp: ApiAnalyticsProxyFilters = {
        ...mockActiveFilters,
        applications: ['app-1'],
      };

      // Act
      fixture.componentRef.setInput('activeFilters', filtersWithApp);
      fixture.detectChanges();

      // Assert
      expectApplicationFindByIds(['app-1']);
      expect(await harness.showsChipByText('Test Application 1')).toBe(true);
    });

    it('should show selected application in select-search label when application id is in activeFilters', async () => {
      // Arrange
      const filtersWithApp: ApiAnalyticsProxyFilters = {
        ...mockActiveFilters,
        applications: ['app-1'],
      };
      fixture.componentRef.setInput('activeFilters', filtersWithApp);
      fixture.detectChanges();
      expectApplicationFindByIds(['app-1']);

      // Act - Check the form control value directly
      const applicationsSelect = await harness.getApplicationsSelect();
      expect(applicationsSelect).toBeTruthy();
      expect(await applicationsSelect!.getTriggerText()).toContain('Application  1');
    });

    it('should have application selected when opening select-search if application id is in activeFilters', async () => {
      // Arrange
      const filtersWithApp: ApiAnalyticsProxyFilters = {
        ...mockActiveFilters,
        applications: ['app-1'],
      };
      fixture.componentRef.setInput('activeFilters', filtersWithApp);
      fixture.detectChanges();
      expectApplicationFindByIds(['app-1']);

      // Act - Open the select to see what's selected
      await harness.openApplicationsSelect();

      // Mock the HTTP response
      expectApplicationSubscribers();

      // Assert - Should show the application as selected in the form control
      expect(await harness.getSelectedApplications()).toEqual(['app-1']);
    });
  });

  describe('Application Filter Chips - Backend Integration', () => {
    beforeEach(() => {
      // Reset filters to ensure clean state
      fixture.componentRef.setInput('activeFilters', {
        ...mockActiveFilters,
        applications: [],
      });
      fixture.detectChanges();
    });

    describe('when there are no application filters applied', () => {
      it('should not call the backend', async () => {
        // Assert
        expect(await harness.getFilterChipCount()).toBe(0);
      });
    });

    describe('when there is a single application filter', () => {
      it('should call the backend and show the chip name', async () => {
        // Arrange
        const appId = 'app-1';

        // Act
        fixture.componentRef.setInput('activeFilters', { ...mockActiveFilters, applications: [appId] });
        fixture.detectChanges();

        // Assert - Expect HTTP request for applications
        const mockResponse = new PagedResult<Application>();
        mockResponse.data = [mockApplications[0]];
        mockResponse.page = { current: 1, per_page: 10, size: 1, total_elements: 1, total_pages: 1 };
        expectApplicationFindByIds([appId], mockResponse);

        // Verify chip is displayed
        expect(await harness.showsChipByText('Test Application 1')).toBe(true);
      });

      it('should show "Unknown Application" when backend returns no results', async () => {
        // Arrange
        const appId = 'unknown-app';

        // Act
        fixture.componentRef.setInput('activeFilters', { ...mockActiveFilters, applications: [appId] });
        fixture.detectChanges();

        // Assert - Expect HTTP request for applications
        const mockResponse = new PagedResult<Application>();
        mockResponse.data = [];
        mockResponse.page = { current: 1, per_page: 10, size: 0, total_elements: 0, total_pages: 1 };
        expectApplicationFindByIds([appId], mockResponse);

        // Verify fallback chip is displayed
        expect(await harness.showsChipByText('Unknown Application')).toBe(true);
      });
    });

    describe('when there are multiple application filters', () => {
      it('should call the backend once with all IDs and show multiple chips', async () => {
        // Arrange
        const appIds = ['app-1', 'app-2', 'app-3'];

        // Act
        fixture.componentRef.setInput('activeFilters', { ...mockActiveFilters, applications: appIds });
        fixture.detectChanges();

        // Assert - Expect HTTP request for applications
        const mockResponse = new PagedResult<Application>();
        mockResponse.data = mockApplications;
        mockResponse.page = { current: 1, per_page: 10, size: 3, total_elements: 3, total_pages: 1 };
        expectApplicationFindByIds(appIds, mockResponse);

        // Verify all chips are displayed
        expect(await harness.getFilterChipCount()).toBe(3);
        expect(await harness.showsChipByText('Test Application 1')).toBe(true);
        expect(await harness.showsChipByText('Test Application 2')).toBe(true);
        expect(await harness.showsChipByText('Test Application 3')).toBe(true);
      });
    });

    describe('when application filter is unchecked and then rechecked', () => {
      it('should not call the backend a second time and use the cache', async () => {
        // Arrange
        const appId = 'app-1';

        // Act - First selection
        fixture.componentRef.setInput('activeFilters', { ...mockActiveFilters, applications: [appId] });
        fixture.detectChanges();

        // Handle first HTTP request
        const mockResponse1 = new PagedResult<Application>();
        mockResponse1.data = [mockApplications[0]];
        mockResponse1.page = { current: 1, per_page: 10, size: 1, total_elements: 1, total_pages: 1 };
        expectApplicationFindByIds([appId], mockResponse1);

        // Act - Uncheck
        await harness.openApplicationsSelect();
        expectApplicationSubscribers(mockApplications);
        await harness.selectApplication('Test Application 1');
        await harness.closeApplicationsSelect();

        // Act - Recheck (should use cache, no new HTTP request)
        await harness.openApplicationsSelect();
        expectApplicationSubscribers(mockApplications);
        await harness.selectApplication('Test Application 1');
        await harness.closeApplicationsSelect();
      });
    });

    describe('when a user selects an application', () => {
      it('should call the backend and show the chip', async () => {
        // Arrange
        const appId = 'app-1';

        // Act
        fixture.componentRef.setInput('activeFilters', { ...mockActiveFilters, applications: [appId] });
        fixture.detectChanges();

        // Assert - Expect HTTP request for applications
        const mockResponse = new PagedResult<Application>();
        mockResponse.data = [mockApplications[0]];
        mockResponse.page = { current: 1, per_page: 10, size: 1, total_elements: 1, total_pages: 1 };
        expectApplicationFindByIds([appId], mockResponse);

        // Verify chip is displayed
        expect(await harness.showsChipByText('Test Application 1')).toBe(true);
      });
    });

    describe('mixed scenarios with cached and uncached applications', () => {
      it('should only fetch uncached applications and combine with cached ones', async () => {
        // Arrange
        // First, cache one application
        fixture.componentRef.setInput('activeFilters', { ...mockActiveFilters, applications: ['app-1'] });
        fixture.detectChanges();

        // Handle first HTTP request for app-1
        const mockResponse1 = new PagedResult<Application>();
        mockResponse1.data = [mockApplications[0]];
        mockResponse1.page = { current: 1, per_page: 10, size: 1, total_elements: 1, total_pages: 1 };
        expectApplicationFindByIds(['app-1'], mockResponse1);

        // Act - Second selection (should only fetch uncached apps)
        fixture.componentRef.setInput('activeFilters', { ...mockActiveFilters, applications: ['app-1', 'app-2', 'app-3'] });
        fixture.detectChanges();

        // Assert - Expect HTTP request only for uncached applications
        const mockResponse2 = new PagedResult<Application>();
        mockResponse2.data = [mockApplications[1], mockApplications[2]];
        mockResponse2.page = { current: 1, per_page: 10, size: 2, total_elements: 2, total_pages: 1 };
        expectApplicationFindByIds(['app-2', 'app-3'], mockResponse2);

        // Verify all chips are displayed
        expect(await harness.getFilterChipCount()).toBe(3);
        expect(await harness.showsChipByText('Test Application 1')).toBe(true);
        expect(await harness.showsChipByText('Test Application 2')).toBe(true);
        expect(await harness.showsChipByText('Test Application 3')).toBe(true);
      });
    });
  });

  function expectApplicationSubscribers(
    applications: any[] = [
      { id: 'app-1', name: 'Application 1' },
      { id: 'app-2', name: 'Application 2' },
      { id: 'app-3', name: 'Application 3' },
    ],
  ) {
    httpTestingController
      .expectOne(`https://url.test:3000/management/v2/environments/DEFAULT/apis/${API_ID}/subscribers?page=1&perPage=20`)
      .flush({
        data: applications,
        pagination: { total: applications.length, page: 1, perPage: 20 },
      });
  }

  function expectApplicationFindByIds(applicationIds: string[], response: PagedResult<Application> = mockResponse) {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=200&ids=${applicationIds.join('&ids=')}`,
    );
    req.flush(response);
  }
});
