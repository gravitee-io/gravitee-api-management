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

import { ApiAnalyticsNativeFilterBarComponent, ApiAnalyticsNativeFilters } from './api-analytics-native-filter-bar.component';
import { ApiAnalyticsNativeFilterBarHarness } from './api-analytics-native-filter-bar.harness';

import { DATE_TIME_FORMATS } from '../../../../../../shared/utils/timeFrameRanges';
import { GioTestingModule } from '../../../../../../shared/testing';

describe('ApiAnalyticsNativeFilterBarComponent', () => {
  let component: ApiAnalyticsNativeFilterBarComponent;
  let fixture: ComponentFixture<ApiAnalyticsNativeFilterBarComponent>;
  let harness: ApiAnalyticsNativeFilterBarHarness;

  const mockActiveFilters: ApiAnalyticsNativeFilters = {
    period: '1d',
    from: null,
    to: null,
    plans: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsNativeFilterBarComponent, GioTestingModule],
      providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiAnalyticsNativeFilterBarComponent);
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

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsNativeFilterBarHarness);
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

  describe('Selected Filter Chips', () => {
    let emitSpy: jest.SpyInstance;

    const filters: ApiAnalyticsNativeFilters = {
      ...mockActiveFilters,
      plans: ['plan1', 'plan2'],
    };

    beforeEach(() => {
      emitSpy = jest.spyOn(component.filtersChange, 'emit');
      component.form.controls.plans.setValue(['plan1', 'plan2']);
      fixture.componentRef.setInput('activeFilters', filters);
      fixture.detectChanges();
    });

    it('should create filter chips from activeFilters using computed signals', () => {
      expect(component.currentFilterChips()).toHaveLength(2);
      expect(component.currentFilterChips()).toEqual([
        { key: 'plans', value: 'plan1', display: 'plan1' },
        { key: 'plans', value: 'plan2', display: 'plan2' },
      ]);

      expect(component.isFiltering()).toBeTruthy();
    });

    it('should remove any filter and update form', () => {
      component.removeFilter('plans', 'plan1');

      expect(component.form.controls.plans.value).toEqual(['plan2']);
      expect(emitSpy).toHaveBeenCalledWith({
        ...filters,
        plans: ['plan2'],
      });
    });

    it('should reset all filters', () => {
      component.resetAllFilters();

      expect(emitSpy).toHaveBeenCalledWith({
        ...filters,
        plans: null,
      });
    });

    it('should update filter chips when activeFilters change', () => {
      // Arrange
      const newFilters: ApiAnalyticsNativeFilters = {
        ...mockActiveFilters,
        plans: ['plan1'],
      };

      // Act
      fixture.componentRef.setInput('activeFilters', newFilters);
      fixture.detectChanges();

      // Assert
      expect(component.currentFilterChips()).toHaveLength(1);
      expect(component.currentFilterChips()).toEqual([{ key: 'plans', value: 'plan1', display: 'plan1' }]);
      expect(component.isFiltering()).toBeTruthy();
    });

    it('should handle empty filter state correctly', () => {
      // Arrange
      const emptyFilters: ApiAnalyticsNativeFilters = {
        ...mockActiveFilters,
        plans: null,
      };

      // Act
      fixture.componentRef.setInput('activeFilters', emptyFilters);
      fixture.detectChanges();

      // Assert
      expect(component.currentFilterChips()).toHaveLength(0);
      expect(component.isFiltering()).toBeFalsy();
    });
  });
});
