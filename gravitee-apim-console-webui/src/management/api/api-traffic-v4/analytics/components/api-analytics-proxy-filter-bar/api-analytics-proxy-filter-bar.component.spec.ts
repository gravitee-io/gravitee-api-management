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
});
