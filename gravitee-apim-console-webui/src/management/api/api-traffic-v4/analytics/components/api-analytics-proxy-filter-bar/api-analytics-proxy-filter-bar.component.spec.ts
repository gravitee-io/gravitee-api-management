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

import { ApiAnalyticsProxyFilterBarComponent, ApiAnalyticsProxyFilters } from './api-analytics-proxy-filter-bar.component';
import { ApiAnalyticsProxyFilterBarHarness } from './api-analytics-proxy-filter-bar.harness';

import { DATE_TIME_FORMATS } from '../../../../../../shared/utils/timeFrameRanges';
import { GioTestingModule } from '../../../../../../shared/testing';

describe('ApiAnalyticsProxyFilterBarComponent', () => {
  let component: ApiAnalyticsProxyFilterBarComponent;
  let fixture: ComponentFixture<ApiAnalyticsProxyFilterBarComponent>;
  let harness: ApiAnalyticsProxyFilterBarHarness;

  const mockActiveFilters: ApiAnalyticsProxyFilters = {
    period: '1d',
    from: null,
    to: null,
    httpStatuses: [],
    applications: [],
    plans: [],
    hosts: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsProxyFilterBarComponent, GioTestingModule],
      providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiAnalyticsProxyFilterBarComponent);
    fixture.componentRef.setInput('activeFilters', mockActiveFilters);
    component = fixture.componentInstance;

    // Initialize form with mock data
    component.form.patchValue({
      period: mockActiveFilters.period,
      from: mockActiveFilters.from ? moment(mockActiveFilters.from) : null,
      to: mockActiveFilters.to ? moment(mockActiveFilters.to) : null,
      plans: mockActiveFilters.plans,
    });

    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsProxyFilterBarHarness);
  });

  describe('Date Range Validation', () => {
    it('should validate that to date is not before from date', () => {
      // Arrange
      const fromDate = moment();
      const toDate = moment().subtract(1, 'day'); // Before from date

      // Act
      component.form.patchValue({
        from: fromDate,
        to: toDate,
      });

      // Assert
      expect(component.form.hasError('dateRange')).toBe(true);
    });

    it('should pass validation when to date is after from date', () => {
      // Arrange
      const fromDate = moment();
      const toDate = moment().add(1, 'day'); // After from date

      // Act
      component.form.patchValue({
        from: fromDate,
        to: toDate,
      });

      // Assert
      expect(component.form.hasError('dateRange')).toBe(false);
    });

    it('should pass validation when only one date is set', () => {
      // Arrange & Act
      component.form.patchValue({
        from: moment(),
        to: null,
      });

      // Assert
      expect(component.form.hasError('dateRange')).toBe(false);
    });
  });

  describe('Filter Change Output', () => {
    it('should emit filtersChange when period changes', () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');
      const newPeriod = '7d';

      // Act
      component.form.controls.period.setValue(newPeriod);

      // Assert
      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        period: newPeriod,
      });
    });

    it('should emit filtersChange when custom timeframe is applied', () => {
      // Arrange
      const spy = jest.spyOn(component.filtersChange, 'emit');
      const fromDate = moment().subtract(1, 'day');
      const toDate = moment();

      // Act
      component.form.patchValue({
        from: fromDate,
        to: toDate,
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
      component.form.controls.period.setValue('custom');

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
  });

  describe('Form State', () => {
    it('should disable apply button when form is invalid', async () => {
      // Arrange
      const fromDate = moment();
      const toDate = moment().subtract(1, 'day'); // Invalid: to before from

      // Act
      fixture.componentRef.setInput('activeFilters', {
        from: fromDate,
        to: toDate,
        period: 'custom',
      });
      fixture.detectChanges();

      // Assert
      expect(await harness.isApplyButtonEnabled()).toBe(false);
      expect(await harness.hasDateRangeError()).toBe(true);
    });

    it('should enable apply button when form is valid and dates are set', async () => {
      // Arrange
      const fromDate = moment();
      const toDate = moment().add(1, 'day'); // Valid: to after from

      // Act
      fixture.componentRef.setInput('activeFilters', {
        from: fromDate,
        to: toDate,
        period: 'custom',
      });
      fixture.detectChanges();

      // Assert
      expect(await harness.hasDateRangeError()).toBe(false);
    });

    it('should disable apply button when dates are not set', async () => {
      // Arrange & Act
      component.form.patchValue({
        from: null,
        to: null,
        period: 'custom',
      });
      fixture.detectChanges();

      // Assert
      expect(await harness.isApplyButtonEnabled()).toBe(false);
    });
  });

  describe('Selected Filter Chips', () => {
    let emitSpy: jest.SpyInstance;

    const filters: ApiAnalyticsProxyFilters = {
      ...mockActiveFilters,
      httpStatuses: ['200', '404'],
      plans: ['plan1', 'plan2'],
    };

    beforeEach(() => {
      emitSpy = jest.spyOn(component.filtersChange, 'emit');
      component.form.controls.httpStatuses.setValue(['200', '404']);
      component.form.controls.plans.setValue(['plan1', 'plan2']);
      fixture.componentRef.setInput('activeFilters', filters);
      fixture.detectChanges();
    });

    it('should create filter chips from activeFilters using computed signals', () => {
      expect(component.currentFilterChips()).toHaveLength(4);
      expect(component.currentFilterChips()).toEqual([
        { key: 'httpStatuses', value: '200', display: '200 - Ok' },
        { key: 'httpStatuses', value: '404', display: '404 - Not Found' },
        { key: 'plans', value: 'plan1', display: 'plan1' },
        { key: 'plans', value: 'plan2', display: 'plan2' },
      ]);

      expect(component.isFiltering()).toBeTruthy();
    });

    it('should remove any filter and update form', () => {
      component.removeFilter('httpStatuses', '200');

      expect(component.form.controls.httpStatuses.value).toEqual(['404']);
      expect(emitSpy).toHaveBeenCalledWith({
        ...filters,
        httpStatuses: ['404'],
      });
    });

    it('should reset all filters', () => {
      component.resetAllFilters();

      expect(emitSpy).toHaveBeenCalledWith({
        ...filters,
        httpStatuses: null,
        plans: null,
      });
    });

    it('should update filter chips when activeFilters change', () => {
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
      expect(component.currentFilterChips()).toHaveLength(2);
      expect(component.currentFilterChips()).toEqual([
        { key: 'httpStatuses', value: '500', display: '500 - Internal Server Error' },
        { key: 'plans', value: 'plan1', display: 'plan1' },
      ]);
      expect(component.isFiltering()).toBeTruthy();
    });
  });
});
