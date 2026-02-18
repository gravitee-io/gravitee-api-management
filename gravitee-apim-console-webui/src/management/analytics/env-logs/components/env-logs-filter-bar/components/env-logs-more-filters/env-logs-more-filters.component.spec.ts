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
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import moment from 'moment';

import { EnvLogsMoreFiltersComponent } from './env-logs-more-filters.component';
import { EnvLogsMoreFiltersHarness } from './env-logs-more-filters.harness';

import { DEFAULT_MORE_FILTERS } from '../../../../models/env-logs-more-filters.model';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../../shared/testing';

describe('EnvLogsMoreFiltersComponent', () => {
  let fixture: ComponentFixture<EnvLogsMoreFiltersComponent>;
  let harness: EnvLogsMoreFiltersHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvLogsMoreFiltersComponent, GioTestingModule],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EnvLogsMoreFiltersComponent);
    fixture.componentRef.setInput('showMoreFilters', true);
    fixture.componentRef.setInput('formValues', DEFAULT_MORE_FILTERS);
    fixture.componentRef.setInput('selectedApiIds', []);
    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvLogsMoreFiltersHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
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

  describe('plans dropdown', () => {
    it('should hide plans when no API is selected', async () => {
      const plansSelect = await harness.getPlans();
      expect(plansSelect).toBeNull();
    });

    it('should hide plans when multiple APIs are selected', async () => {
      fixture.componentRef.setInput('selectedApiIds', ['api-1', 'api-2']);
      fixture.detectChanges();

      const plansSelect = await harness.getPlans();
      expect(plansSelect).toBeNull();
    });

    it('should show plans when exactly one API is selected', async () => {
      fixture.componentRef.setInput('selectedApiIds', ['api-1']);
      fixture.detectChanges();

      // Flush the plans HTTP request
      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-1/plans?page=1&perPage=100`,
      });
      req.flush({ data: [{ id: 'plan-1', name: 'Gold Plan' }], pagination: { page: 1, perPage: 9999, totalCount: 1 } });
      fixture.detectChanges();

      const plansSelect = await harness.getPlans();
      expect(plansSelect).toBeTruthy();

      await plansSelect!.open();
      const options = await plansSelect!.getOptions();
      expect(options.length).toBe(1);
      expect(await options[0].getText()).toBe('Gold Plan');
    });

    it('should include selected plans in apply emission', async () => {
      fixture.componentRef.setInput('selectedApiIds', ['api-1']);
      fixture.detectChanges();

      // Flush plans request
      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/api-1/plans?page=1&perPage=100`,
      });
      req.flush({ data: [{ id: 'plan-1', name: 'Gold Plan' }], pagination: { page: 1, perPage: 9999, totalCount: 1 } });
      fixture.detectChanges();

      const applySpy = jest.fn();
      fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

      const plansSelect = (await harness.getPlans())!;
      await plansSelect.open();
      const [option] = await plansSelect.getOptions({ text: 'Gold Plan' });
      await option.click();

      await harness.clickApply();

      expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ plans: ['plan-1'] }));
    });
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

  it('should include Transaction ID in apply emission (valid UUID)', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const txInput = (await harness.getTransactionId())!;
    expect(txInput).toBeTruthy();
    await txInput.setValue('a1b2c3d4-e5f6-7890-abcd-ef1234567890');

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ transactionId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890' }));
  });

  it('should include Request ID in apply emission (valid UUID)', async () => {
    const applySpy = jest.fn();
    fixture.componentInstance.applyMoreFilters.subscribe(applySpy);

    const reqInput = (await harness.getRequestId())!;
    expect(reqInput).toBeTruthy();
    await reqInput.setValue('f0e1d2c3-b4a5-6789-0123-456789abcdef');

    await harness.clickApply();

    expect(applySpy).toHaveBeenCalledWith(expect.objectContaining({ requestId: 'f0e1d2c3-b4a5-6789-0123-456789abcdef' }));
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

  describe('input validation', () => {
    it('should disable apply when transaction ID is not a valid UUID', async () => {
      const txInput = (await harness.getTransactionId())!;
      await txInput.setValue('not-a-uuid');

      expect(await harness.isApplyDisabled()).toBe(true);
    });

    it('should disable apply when request ID is not a valid UUID', async () => {
      const reqInput = (await harness.getRequestId())!;
      await reqInput.setValue('not-a-uuid');

      expect(await harness.isApplyDisabled()).toBe(true);
    });

    it('should disable apply when response time is negative', async () => {
      const rtInput = (await harness.getResponseTime())!;
      await rtInput.setValue('-1');

      expect(await harness.isApplyDisabled()).toBe(true);
    });

    it('should allow apply when all fields are valid', async () => {
      expect(await harness.isApplyDisabled()).toBe(false);
    });
  });

  describe('addStatusFromInput', () => {
    it('should add a valid numeric status (100-599)', () => {
      const event = { value: '200', chipInput: { clear: jest.fn() } } as any;

      fixture.componentInstance.addStatusFromInput(event);

      expect(fixture.componentInstance.statuses.has(200)).toBe(true);
      expect(event.chipInput.clear).toHaveBeenCalled();
    });

    it('should reject status codes below 100', () => {
      const event = { value: '99', chipInput: { clear: jest.fn() } } as any;

      fixture.componentInstance.addStatusFromInput(event);

      expect(fixture.componentInstance.statuses.size).toBe(0);
      expect(event.chipInput.clear).toHaveBeenCalled();
    });

    it('should reject status codes above 599', () => {
      const event = { value: '600', chipInput: { clear: jest.fn() } } as any;

      fixture.componentInstance.addStatusFromInput(event);

      expect(fixture.componentInstance.statuses.size).toBe(0);
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
  });

  describe('formValues input sync', () => {
    it('should sync form when formValues input changes', () => {
      const newValues = {
        ...DEFAULT_MORE_FILTERS,
        entrypoints: ['http-proxy'],
        from: moment('2026-01-15T10:00:00'),
      };

      fixture.componentRef.setInput('formValues', newValues);
      fixture.detectChanges();

      expect(fixture.componentInstance.form.value.entrypoints).toEqual(['http-proxy']);
      expect(fixture.componentInstance.minDate).toBeTruthy();
    });
  });
});
