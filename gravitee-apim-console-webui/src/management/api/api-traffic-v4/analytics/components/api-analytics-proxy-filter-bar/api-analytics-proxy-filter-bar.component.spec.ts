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
    fixture.componentRef.setInput('plans', ['planId1', 'planId2']);
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

  describe('Applied Filters', () => {
    describe('hasAppliedFilters', () => {
      it('should return false when no filters are applied', () => {
        fixture.componentRef.setInput('activeFilters', {
          period: '1d',
          from: null,
          to: null,
          httpStatuses: null,
          plans: null,
          hosts: null,
          applications: null,
        });

        expect(component.hasAppliedFilters()).toBe(false);
      });

      it('should return true when httpStatuses are applied', () => {
        fixture.componentRef.setInput('activeFilters', {
          period: '1d',
          from: null,
          to: null,
          httpStatuses: ['200'],
          plans: null,
          hosts: null,
          applications: null,
        });

        expect(component.hasAppliedFilters()).toBe(true);
      });

      it('should return true when plans are applied', () => {
        fixture.componentRef.setInput('activeFilters', {
          period: '1d',
          from: null,
          to: null,
          httpStatuses: null,
          plans: ['plan1'],
          hosts: null,
          applications: null,
        });

        expect(component.hasAppliedFilters()).toBe(true);
      });

      it('should return false when only date filters are applied (dates are part of timeframe, not shown as chips)', () => {
        fixture.componentRef.setInput('activeFilters', {
          period: 'custom',
          from: 1234567890000,
          to: 1234567890000,
          httpStatuses: null,
          plans: null,
          hosts: null,
          applications: null,
        });

        expect(component.hasAppliedFilters()).toBe(false);
      });

      it('should return true when hosts are applied', () => {
        fixture.componentRef.setInput('activeFilters', {
          period: '1d',
          from: null,
          to: null,
          httpStatuses: null,
          plans: null,
          hosts: ['host1'],
          applications: null,
        });

        expect(component.hasAppliedFilters()).toBe(true);
      });

      it('should return true when applications are applied', () => {
        fixture.componentRef.setInput('activeFilters', {
          period: '1d',
          from: null,
          to: null,
          httpStatuses: null,
          plans: null,
          hosts: null,
          applications: ['app1'],
        });

        expect(component.hasAppliedFilters()).toBe(true);
      });

      it('should return false when only period is different (period is not counted as filter)', () => {
        fixture.componentRef.setInput('activeFilters', {
          period: '7d',
          from: null,
          to: null,
          httpStatuses: null,
          plans: null,
          hosts: null,
          applications: null,
        });

        expect(component.hasAppliedFilters()).toBe(false);
      });
    });

    describe('removeFilter', () => {
      it('should remove individual item from array', () => {
        const spy = jest.spyOn(component.filtersChange, 'emit');
        const testFilters: ApiAnalyticsProxyFilters = {
          period: '7d',
          from: null,
          to: null,
          httpStatuses: ['200', '404'],
          plans: ['plan1'],
          hosts: null,
          applications: null,
        };
        fixture.componentRef.setInput('activeFilters', testFilters);

        component.removeFilter('httpStatuses', '200');

        expect(spy).toHaveBeenCalledWith({
          period: '7d',
          from: null,
          to: null,
          httpStatuses: ['404'],
          plans: ['plan1'],
          hosts: null,
          applications: null,
        });
      });

      it('should reset filter to null when removing last item', () => {
        const spy = jest.spyOn(component.filtersChange, 'emit');
        const testFilters: ApiAnalyticsProxyFilters = {
          period: '1d',
          from: null,
          to: null,
          httpStatuses: ['200'],
          plans: null,
          hosts: null,
          applications: null,
        };
        fixture.componentRef.setInput('activeFilters', testFilters);

        component.removeFilter('httpStatuses', '200');

        expect(spy).toHaveBeenCalledWith({
          period: '1d',
          from: null,
          to: null,
          httpStatuses: null,
          plans: null,
          hosts: null,
          applications: null,
        });
      });
    });

    describe('resetAllFilters', () => {
      it('should reset only filters while keeping timeframe intact', () => {
        const spy = jest.spyOn(component.filtersChange, 'emit');
        const testFilters: ApiAnalyticsProxyFilters = {
          period: '7d',
          from: 1234567890000,
          to: 1234567890000,
          httpStatuses: ['200', '404'],
          plans: ['plan1'],
          hosts: ['host1'],
          applications: ['app1'],
        };
        fixture.componentRef.setInput('activeFilters', testFilters);

        component.resetAllFilters();

        expect(spy).toHaveBeenCalledWith({
          period: '7d', // Timeframe preserved
          from: 1234567890000, // Timeframe preserved
          to: 1234567890000, // Timeframe preserved
          httpStatuses: null, // Filter reset
          plans: null, // Filter reset
          hosts: null, // Filter reset
          applications: null, // Filter reset
        });
      });
    });
  });
});
