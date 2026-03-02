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
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import moment from 'moment';

import { EnvLogsMoreFiltersComponent } from './env-logs-more-filters.component';
import { EnvLogsMoreFiltersHarness } from './env-logs-more-filters.harness';

import { DEFAULT_MORE_FILTERS } from '../../../../models/env-logs-more-filters.model';
import { AnalyticsService } from '../../../../../../../services-ngx/analytics.service';

describe('EnvLogsMoreFiltersComponent', () => {
  let fixture: ComponentFixture<EnvLogsMoreFiltersComponent>;
  let harness: EnvLogsMoreFiltersHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvLogsMoreFiltersComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        {
          provide: AnalyticsService,
          useValue: {
            getEnvironmentErrorKeys: jest.fn().mockReturnValue(of(['error-key-1'])),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvLogsMoreFiltersComponent);
    fixture.componentRef.setInput('showMoreFilters', true);
    fixture.componentRef.setInput('formValues', DEFAULT_MORE_FILTERS);
    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvLogsMoreFiltersHarness);
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display from date input when panel is open', async () => {
    const fromInput = fixture.nativeElement.querySelector('[data-testid="more-filters-from"]');
    expect(fromInput).toBeTruthy();
  });

  it('should display status input when panel is open', async () => {
    expect(await harness.hasStatusInput()).toBe(true);
  });

  it('should emit apply and close events when clicking Show results', async () => {
    const applySpy = jest.fn();
    const closeSpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);
    fixture.componentInstance.closeMoreFilters.subscribe(closeSpy);

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ from: null, to: null }));
    expect(closeSpy).toHaveBeenCalled();
  });

  it('should reset values and emit when clicking Clear all', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    await harness.clickClearAll();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ from: null, to: null }));
  });

  it('should emit close event when clicking the close button', async () => {
    const closeSpy = jest.fn();
    fixture.componentInstance.closeMoreFilters.subscribe(closeSpy);

    await harness.clickClose();

    expect(closeSpy).toHaveBeenCalled();
  });

  it('should display entrypoints select when panel is open', async () => {
    const entrypointsSelect = await harness.getEntrypoints();
    expect(entrypointsSelect).toBeTruthy();
  });

  it('should display HTTP methods select when panel is open', async () => {
    const methodsSelect = await harness.getMethods();
    expect(methodsSelect).toBeTruthy();
  });

  it('should display plans select when panel is open', async () => {
    const plansSelect = await harness.getPlans();
    expect(plansSelect).toBeTruthy();
  });

  it('should include selected entrypoints in apply emission', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const entrypointsSelect = (await harness.getEntrypoints())!;
    await entrypointsSelect.open();
    const [option] = await entrypointsSelect.getOptions({ text: 'HTTP Proxy' });
    await option.click();

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ entrypoints: ['http-proxy'] }));
  });

  it('should include selected methods in apply emission', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const methodsSelect = (await harness.getMethods())!;
    await methodsSelect.open();
    const [option] = await methodsSelect.getOptions({ text: 'GET' });
    await option.click();

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ methods: ['GET'] }));
  });

  it('should include selected plans in apply emission', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const plansSelect = (await harness.getPlans())!;
    await plansSelect.open();
    const [option] = await plansSelect.getOptions({ text: 'Gold Plan' });
    await option.click();

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ plans: ['plan-2'] }));
  });

  it('should include MCP method in apply emission', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const mcpInput = (await harness.getMcpMethod())!;
    expect(mcpInput).toBeTruthy();
    await mcpInput.setValue('tools/list');

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ mcpMethod: 'tools/list' }));
  });

  it('should include Transaction ID in apply emission', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const txInput = (await harness.getTransactionId())!;
    expect(txInput).toBeTruthy();
    await txInput.setValue('tx-abc-123');

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ transactionId: 'tx-abc-123' }));
  });

  it('should include Request ID in apply emission', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const reqInput = (await harness.getRequestId())!;
    expect(reqInput).toBeTruthy();
    await reqInput.setValue('req-xyz-456');

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ requestId: 'req-xyz-456' }));
  });

  it('should include URI in apply emission', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const uriInput = (await harness.getUri())!;
    expect(uriInput).toBeTruthy();
    await uriInput.setValue('/api/v1/users');

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ uri: '/api/v1/users' }));
  });

  it('should include response time in apply emission', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const rtInput = (await harness.getResponseTime())!;
    expect(rtInput).toBeTruthy();
    await rtInput.setValue('500');

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ responseTime: 500 }));
  });

  describe('addStatusFromInput', () => {
    it('should add a valid numeric status', () => {
      const event = { value: '200', chipInput: { clear: jest.fn() } } as any;

      fixture.componentInstance.addStatusFromInput(event);

      expect(fixture.componentInstance.statuses.has(200)).toBe(true);
      expect(event.chipInput.clear).toHaveBeenCalled();
    });

    it('should not add an invalid non-numeric status', () => {
      const event = { value: 'abc', chipInput: { clear: jest.fn() } } as any;

      fixture.componentInstance.addStatusFromInput(event);

      expect(fixture.componentInstance.statuses.size).toBe(0);
      expect(event.chipInput.clear).not.toHaveBeenCalled();
    });

    it('should not add an empty string status', () => {
      const event = { value: '  ', chipInput: { clear: jest.fn() } } as any;

      fixture.componentInstance.addStatusFromInput(event);

      expect(fixture.componentInstance.statuses.size).toBe(0);
    });
  });

  describe('removeStatus', () => {
    it('should remove a status from the statuses set', () => {
      fixture.componentInstance.statuses = new Set([200, 404]);

      fixture.componentInstance.removeStatus(200);

      expect(fixture.componentInstance.statuses.has(200)).toBe(false);
      expect(fixture.componentInstance.statuses.has(404)).toBe(true);
    });
  });

  describe('minDateDisplay', () => {
    it('should return null when minDate is null', () => {
      fixture.componentInstance.minDate = null;

      expect(fixture.componentInstance.minDateDisplay).toBeNull();
    });

    it('should return a Date object when minDate is set', () => {
      fixture.componentInstance.minDate = moment('2026-01-15T10:00:00');

      expect(fixture.componentInstance.minDateDisplay).toBeInstanceOf(Date);
    });

    it('should update minDate when the from control value changes', () => {
      const fromDate = moment('2026-03-01T09:00:00');

      fixture.componentInstance.form.controls.from.setValue(fromDate);
      fixture.detectChanges();

      expect(fixture.componentInstance.minDate).toEqual(fromDate);
    });

    it('should set minDate to null when from control is cleared', () => {
      fixture.componentInstance.form.controls.from.setValue(moment('2026-03-01T09:00:00'));
      fixture.detectChanges();

      fixture.componentInstance.form.controls.from.setValue(null);
      fixture.detectChanges();

      expect(fixture.componentInstance.minDate).toBeNull();
    });
  });

  describe('formValues input sync', () => {
    it('should sync form when formValues input changes', () => {
      const newValues = {
        ...DEFAULT_MORE_FILTERS,
        mcpMethod: 'tools/call',
        entrypoints: ['http-proxy'],
        from: moment('2026-01-15T10:00:00'),
      };

      fixture.componentRef.setInput('formValues', newValues);
      fixture.detectChanges();

      expect(fixture.componentInstance.form.value.mcpMethod).toBe('tools/call');
      expect(fixture.componentInstance.form.value.entrypoints).toEqual(['http-proxy']);
      expect(fixture.componentInstance.minDate).toBeTruthy();
    });

    it('should sync the statuses set when formValues input changes', () => {
      fixture.componentRef.setInput('formValues', { ...DEFAULT_MORE_FILTERS, statuses: new Set([200, 404]) });
      fixture.detectChanges();

      expect(fixture.componentInstance.statuses.has(200)).toBe(true);
      expect(fixture.componentInstance.statuses.has(404)).toBe(true);
    });
  });
  describe('Error Keys functionality', () => {
    it('should include selected error keys in apply emission', async () => {
      const applySpy = jest.fn();
      fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

      // Directly set the options to simulate service response
      fixture.componentInstance.errorKeysOptions = ['API_KEY_MISSING', 'QUOTA_EXCEEDED'];
      fixture.detectChanges();

      fixture.componentInstance.form.controls.errorKeys.setValue(['API_KEY_MISSING']);

      await harness.clickApply();

      expect(applySpy).toHaveBeenCalledWith(
        expect.objectContaining({
          errorKeys: ['API_KEY_MISSING'],
        }),
      );
    });

    it('should fetch error keys when dates change', done => {
      const analyticsService = TestBed.inject(AnalyticsService);
      const fromDate = moment('2026-01-01');
      const toDate = moment('2026-01-02');

      // Trigger value changes on the form
      fixture.componentInstance.form.controls.from.setValue(fromDate);
      fixture.componentInstance.form.controls.to.setValue(toDate);
      fixture.detectChanges();

      // Wait for debounceTime(200)
      setTimeout(() => {
        expect(analyticsService.getEnvironmentErrorKeys).toHaveBeenCalledWith(fromDate.valueOf(), toDate.valueOf());
        expect(fixture.componentInstance.errorKeysOptions).toEqual(['error-key-1']);
        done();
      }, 300);
    });

    it('should use default time range for error keys if form dates are missing', done => {
      const analyticsService = TestBed.inject(AnalyticsService);

      // Reset form dates
      fixture.componentInstance.form.controls.from.setValue(null);
      fixture.componentInstance.form.controls.to.setValue(null);
      fixture.detectChanges();

      setTimeout(() => {
        // Verify it called with fallback values (last 24h logic)
        expect(analyticsService.getEnvironmentErrorKeys).toHaveBeenCalledWith(expect.any(Number), expect.any(Number));
        done();
      }, 300);
    });
  });

  describe('resetMoreFilters', () => {
    it('should clear all fields including errorKeys and statuses', async () => {
      const applySpy = jest.fn();
      fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

      // Populate form
      fixture.componentInstance.statuses = new Set([500]);
      fixture.componentInstance.form.patchValue({
        mcpMethod: 'test',
        errorKeys: ['KEY'],
      });

      fixture.componentInstance.resetMoreFilters();

      expect(fixture.componentInstance.statuses.size).toBe(0);
      expect(fixture.componentInstance.form.value.errorKeys).toBeNull();
      expect(fixture.componentInstance.form.value.mcpMethod).toBeNull();

      expect(applySpy).toHaveBeenCalledWith(
        expect.objectContaining({
          errorKeys: null,
          statuses: new Set(),
        }),
      );
    });
  });

  describe('apply sanitization', () => {
    it('should trim string values before emitting', () => {
      const applySpy = jest.fn();
      fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

      fixture.componentInstance.form.patchValue({
        mcpMethod: '  tools/list  ',
        transactionId: ' tx-123 ',
      });

      fixture.componentInstance.apply();

      expect(applySpy).toHaveBeenCalledWith(
        expect.objectContaining({
          mcpMethod: 'tools/list',
          transactionId: 'tx-123',
        }),
      );
    });
  });
});
