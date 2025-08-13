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
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsProxyFilterBarComponent, GioTestingModule],
      providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiAnalyticsProxyFilterBarComponent);
    fixture.componentRef.setInput('activeFilters', mockActiveFilters);
    component = fixture.componentInstance;

    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsProxyFilterBarHarness);
  });

  describe('Date Range Validation', () => {
    it('should validate that to date is not before from date', async () => {
      const fromDate = moment();
      const toDate = moment().subtract(1, 'day');

      await harness.selectPeriod('Custom');
      await harness.setCustomDateRange(fromDate, toDate);

      expect(await harness.hasDateRangeError()).toBe(true);
    });

    it('should pass validation when to date is after from date', async () => {
      const fromDate = moment();
      const toDate = moment().add(1, 'day');

      await harness.selectPeriod('Custom');
      await harness.setCustomDateRange(fromDate, toDate);

      expect(await harness.hasDateRangeError()).toBe(false);
    });
  });

  describe('Filter Change Output', () => {
    it('should emit filtersChange when period changes', async () => {
      const spy = jest.spyOn(component.filtersChange, 'emit');

      await harness.selectPeriod('Last week');

      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        period: '1w',
      });
    });

    it('should emit filtersChange when custom timeframe is applied', async () => {
      const spy = jest.spyOn(component.filtersChange, 'emit');
      const fromDate = moment().subtract(1, 'day');
      const toDate = moment();

      await harness.selectPeriod('Custom');
      await harness.setCustomDateRange(fromDate, toDate);
      await harness.clickApply();

      expect(spy).toHaveBeenCalledWith({
        ...mockActiveFilters,
        from: fromDate.valueOf(),
        to: toDate.valueOf(),
        period: 'custom',
      });
    });

    it('should emit filtersChange when refresh is called', async () => {
      const spy = jest.spyOn(component.refresh, 'emit');
      await harness.selectPeriod('Last day');
      await harness.clickRefresh();
      expect(spy).toHaveBeenCalled();
    });

    it('should not emit when period is set to custom without applying', async () => {
      const spy = jest.spyOn(component.filtersChange, 'emit');
      await harness.selectPeriod('Custom');
      expect(spy).not.toHaveBeenCalledWith(expect.objectContaining({ period: 'custom' }));
    });
  });

  describe('Form State', () => {
    it('should disable apply button when form is invalid', async () => {
      const fromDate = moment();
      const toDate = moment().subtract(1, 'day');

      await harness.selectPeriod('Custom');
      await harness.setCustomDateRange(fromDate, toDate);

      expect(await harness.isApplyButtonEnabled()).toBe(false);
      expect(await harness.hasDateRangeError()).toBe(true);
    });

    it('should enable apply button when form is valid and dates are set', async () => {
      const fromDate = moment();
      const toDate = moment().add(1, 'day');

      await harness.selectPeriod('Custom');
      await harness.setCustomDateRange(fromDate, toDate);

      expect(await harness.hasDateRangeError()).toBe(false);
      expect(await harness.isApplyButtonEnabled()).toBe(true);
    });

    it('should disable apply button when dates are not set', async () => {
      await harness.selectPeriod('Custom');
      expect(await harness.isApplyButtonEnabled()).toBe(false);
    });
  });
});
