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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatChipHarness, MatChipSetHarness } from '@angular/material/chips/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import moment from 'moment';

import { EnvLogsFilterBarComponent } from './env-logs-filter-bar.component';

import { DEFAULT_MORE_FILTERS, ENV_LOGS_DEFAULT_PERIOD } from '../../models/env-logs-more-filters.model';
import { GioTestingModule } from '../../../../../shared/testing';
import { AnalyticsService } from '../../../../../services-ngx/analytics.service';

describe('EnvLogsFilterBarComponent', () => {
  let component: EnvLogsFilterBarComponent;
  let fixture: ComponentFixture<EnvLogsFilterBarComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, EnvLogsFilterBarComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: AnalyticsService,
          useValue: { getEnvironmentErrorKeys: jest.fn().mockReturnValue(of([])) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvLogsFilterBarComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display all filter dropdowns', async () => {
    const selects = await loader.getAllHarnesses(MatSelectHarness);
    expect(selects.length).toBe(3); // Period, API, Application
  });

  it('should display refresh button', async () => {
    const refreshButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="refresh-button"]' }));
    expect(refreshButton).toBeTruthy();
  });

  it('should show "No filter applied" when no filters are active', () => {
    const noFilterText = fixture.nativeElement.querySelector('[data-testid="no-filters-text"]');
    expect(noFilterText.textContent).toContain('No filter applied');
  });

  describe('filter selection', () => {
    it('should display a chip when an API is selected', async () => {
      const apisSelect = await loader.getHarness(MatSelectHarness.with({ ancestor: '[data-testid="apis-select"]' }));
      await apisSelect.open();
      const [apiOption] = await apisSelect.getOptions({ text: 'Weather API' });
      await apiOption.click();

      const chipSet = await loader.getHarness(MatChipSetHarness);
      const chips = await chipSet.getChips();
      expect(chips.length).toBe(1);
    });

    it('should display a chip when an Application is selected', async () => {
      const appsSelect = await loader.getHarness(MatSelectHarness.with({ ancestor: '[data-testid="applications-select"]' }));
      await appsSelect.open();
      const [appOption] = await appsSelect.getOptions({ text: 'Mobile App' });
      await appOption.click();

      const chipSet = await loader.getHarness(MatChipSetHarness);
      const chips = await chipSet.getChips();
      expect(chips.length).toBe(1);
    });

    it('should have the default period set on initialization', () => {
      const periodValue = component.form.get('period')?.value;
      expect(periodValue).toEqual(ENV_LOGS_DEFAULT_PERIOD);
    });
  });

  describe('chip removal', () => {
    it('should remove a chip when the remove icon is clicked', async () => {
      // Select an API first
      const apisSelect = await loader.getHarness(MatSelectHarness.with({ ancestor: '[data-testid="apis-select"]' }));
      await apisSelect.open();
      const [apiOption] = await apisSelect.getOptions({ text: 'Weather API' });
      await apiOption.click();

      // Verify chip exists
      const chipSet = await loader.getHarness(MatChipSetHarness);
      const chips = await chipSet.getChips();
      expect(chips.length).toBe(1);

      // Remove the chip
      const chip = await loader.getHarness(MatChipHarness);
      await chip.remove();

      // Verify chip is removed and "No filter applied" appears
      const noFilterText = fixture.nativeElement.querySelector('[data-testid="no-filters-text"]');
      expect(noFilterText).toBeTruthy();
    });
  });

  describe('resetAllFilters', () => {
    it('should clear all filters when reset is clicked', async () => {
      // Select an API first
      const apisSelect = await loader.getHarness(MatSelectHarness.with({ ancestor: '[data-testid="apis-select"]' }));
      await apisSelect.open();
      const [apiOption] = await apisSelect.getOptions({ text: 'Weather API' });
      await apiOption.click();

      // Click reset
      const resetButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="reset-filters-button"]' }));
      await resetButton.click();

      // Verify filters are cleared
      const noFilterText = fixture.nativeElement.querySelector('[data-testid="no-filters-text"]');
      expect(noFilterText).toBeTruthy();
    });
  });

  describe('refresh', () => {
    it('should emit refresh when refresh button is clicked', async () => {
      const refreshSpy = jest.fn();
      component.refresh.subscribe(refreshSpy);

      const refreshButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="refresh-button"]' }));
      await refreshButton.click();

      expect(refreshSpy).toHaveBeenCalled();
    });

    it('should disable refresh button when loading', async () => {
      fixture.componentRef.setInput('loading', true);
      fixture.detectChanges();

      const refreshButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="refresh-button"]' }));
      expect(await refreshButton.isDisabled()).toBe(true);
    });
  });

  describe('removeChip for more filters', () => {
    it('should remove a status chip from the moreFiltersValues signal', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        statuses: new Set([200, 404]),
      });
      fixture.detectChanges();

      component.removeChip({ key: 'statuses', value: 200, display: '200' });

      const statuses = component.moreFiltersValues().statuses;
      expect(statuses.has(200)).toBe(false);
      expect(statuses.has(404)).toBe(true);
    });

    it('should remove a scalar more-filter chip (e.g. mcpMethod) from the moreFiltersValues signal', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        mcpMethod: 'tools/list',
      });
      fixture.detectChanges();

      component.removeChip({ key: 'mcpMethod', value: 'tools/list', display: 'MCP Method: tools/list' });

      expect(component.moreFiltersValues().mcpMethod).toBeNull();
    });

    it('should remove an array more-filter chip (e.g. entrypoints) from the moreFiltersValues signal', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        entrypoints: ['http-proxy', 'sse'],
      });
      fixture.detectChanges();

      component.removeChip({ key: 'entrypoints', value: 'http-proxy', display: 'http-proxy' });

      expect(component.moreFiltersValues().entrypoints).toEqual(['sse']);
    });

    it('should set entrypoints to null when the last chip is removed', () => {
      component.moreFiltersValues.set({ ...component.moreFiltersValues(), entrypoints: ['http-proxy'] });
      fixture.detectChanges();

      component.removeChip({ key: 'entrypoints', value: 'http-proxy', display: 'http-proxy' });

      expect(component.moreFiltersValues().entrypoints).toBeNull();
    });

    it('should remove a from date chip from the moreFiltersValues signal', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        from: moment('2026-01-15T10:00:00'),
      });
      fixture.detectChanges();

      component.removeChip({ key: 'from', value: 'from', display: '2026-01-15 10:00:00' });

      expect(component.moreFiltersValues().from).toBeNull();
    });

    it('should remove a to date chip from the moreFiltersValues signal', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        to: moment('2026-01-20T18:00:00'),
      });
      fixture.detectChanges();

      component.removeChip({ key: 'to', value: 'to', display: '2026-01-20 18:00:00' });

      expect(component.moreFiltersValues().to).toBeNull();
    });

    it('should remove a responseTime chip from the moreFiltersValues signal', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        responseTime: 500,
      });
      fixture.detectChanges();

      component.removeChip({ key: 'responseTime', value: 500, display: 'Response time: >500ms' });

      expect(component.moreFiltersValues().responseTime).toBeNull();
    });

    it('should set apis to null and stop filtering when the last API chip is removed', () => {
      component.form.patchValue({ apis: ['api-1'] });
      fixture.detectChanges();

      component.removeChip({ key: 'apis', value: 'api-1', display: 'Weather API' });

      expect(component.form.value.apis).toBeNull();
      expect(component.isFiltering()).toBe(false);
    });

    it('should keep remaining APIs when one of multiple is removed', () => {
      component.form.patchValue({ apis: ['api-1', 'api-2'] });
      fixture.detectChanges();

      component.removeChip({ key: 'apis', value: 'api-1', display: 'Weather API' });

      expect(component.form.value.apis).toEqual(['api-2']);
    });

    it('should do nothing when chip key does not match any form control or filter category', () => {
      const stateBefore = component.moreFiltersValues();

      component.removeChip({ key: 'unknownKey', value: 'some-value', display: 'some-value' });

      expect(component.moreFiltersValues()).toEqual(stateBefore);
    });

    it('should handle removeChip for a form control whose current value is null', () => {
      // apis is null by default — the ?? [] fallback is exercised
      component.removeChip({ key: 'apis', value: 'api-1', display: 'Weather API' });

      expect(component.form.value.apis).toBeNull();
    });

    it('should handle removeChip for an array more-filter when current array is null', () => {
      // entrypoints is null by default — the ?? [] fallback is exercised
      component.removeChip({ key: 'entrypoints', value: 'http-proxy', display: 'http-proxy' });

      expect(component.moreFiltersValues().entrypoints).toBeNull();
    });
  });

  describe('filterChips computed signal', () => {
    it('should include from and to date chips when dates are set', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        from: moment('2026-01-15T10:00:00'),
        to: moment('2026-01-20T18:00:00'),
      });
      fixture.detectChanges();

      const chips = component.filterChips();
      expect(chips.some(c => c.key === 'from')).toBe(true);
      expect(chips.some(c => c.key === 'to')).toBe(true);
    });

    it('should include a responseTime chip when responseTime is set', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        responseTime: 250,
      });
      fixture.detectChanges();

      const chips = component.filterChips();
      const rtChip = chips.find(c => c.key === 'responseTime');
      expect(rtChip).toBeTruthy();
      expect(rtChip!.display).toBe('Response time: >250ms');
    });

    it('should display the API name (not id) in the chip', () => {
      component.form.patchValue({ apis: ['api-1'] });
      fixture.detectChanges();

      const chip = component.filterChips().find(c => c.key === 'apis');
      expect(chip?.display).toBe('Weather API');
      expect(chip?.value).toBe('api-1');
    });

    it('should not produce chips for errorKeys (they appear in filtersParam only)', () => {
      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, errorKeys: ['TIMEOUT'] });
      fixture.detectChanges();

      expect(component.filterChips().some(c => c.key === 'errorKeys')).toBe(false);
    });

    it('should fall back to the raw id when the API id is not in the name map', () => {
      component.form.patchValue({ apis: ['unknown-api-id'] });
      fixture.detectChanges();

      const chip = component.filterChips().find(c => c.key === 'apis');
      expect(chip?.display).toBe('unknown-api-id');
      expect(chip?.value).toBe('unknown-api-id');
    });
  });

  describe('applyMoreFilters', () => {
    it('should set moreFiltersValues and close the panel', () => {
      component.showMoreFilters.set(true);
      const newValues = {
        ...component.moreFiltersValues(),
        mcpMethod: 'tools/list',
        statuses: new Set([200]),
      };

      component.applyMoreFilters(newValues);

      expect(component.moreFiltersValues().mcpMethod).toBe('tools/list');
      expect(component.moreFiltersValues().statuses.has(200)).toBe(true);
      expect(component.showMoreFilters()).toBe(false);
    });
  });

  describe('comparePeriod', () => {
    it('should return true when both arguments are null', () => {
      expect(component.comparePeriod(null as any, null as any)).toBe(true);
    });

    it('should return false when first argument is null and second has a value', () => {
      expect(component.comparePeriod(null as any, { value: '-1h' })).toBe(false);
    });

    it('should return false when second argument is null and first has a value', () => {
      expect(component.comparePeriod({ value: '-1h' }, null as any)).toBe(false);
    });
  });

  describe('resetAllFilters with more filters', () => {
    it('should reset both quick filters and more-filter values', () => {
      // Set some quick filters
      component.form.patchValue({ apis: ['api-1'] });
      // Set some more filters
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        statuses: new Set([500]),
        mcpMethod: 'tools/call',
        entrypoints: ['http-proxy'],
      });
      fixture.detectChanges();

      component.resetAllFilters();

      expect(component.form.value.apis).toBeNull();
      expect(component.moreFiltersValues().statuses.size).toBe(0);
      expect(component.moreFiltersValues().mcpMethod).toBeNull();
      expect(component.moreFiltersValues().entrypoints).toBeNull();
    });

    it('should reset errorKeys when resetAllFilters is called', () => {
      component.moreFiltersValues.set({ ...component.moreFiltersValues(), errorKeys: ['TIMEOUT'] });
      fixture.detectChanges();

      component.resetAllFilters();

      expect(component.moreFiltersValues().errorKeys).toBeNull();
    });
  });

  describe('filtersParam and filtersChanged', () => {
    it('should emit filtersChanged with an errorKeys filter when errorKeys are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        errorKeys: ['TIMEOUT', 'ASSIGN_CONTENT_ERROR'],
      });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      const errorKeyFilter = last?.filters?.find((f: any) => f.name === 'ERROR_KEY');
      expect(errorKeyFilter).toEqual({ name: 'ERROR_KEY', operator: 'IN', value: ['TIMEOUT', 'ASSIGN_CONTENT_ERROR'] });
    });

    it('should not include an ERROR_KEY filter when errorKeys is empty', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...component.moreFiltersValues(), errorKeys: [] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters?.find((f: any) => f.name === 'ERROR_KEY')).toBeUndefined();
    });

    it('should not include an ERROR_KEY filter when errorKeys is null', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...component.moreFiltersValues(), errorKeys: null });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters?.find((f: any) => f.name === 'ERROR_KEY')).toBeUndefined();
    });

    // NEW: covers the responseTime != null branch
    it('should include a RESPONSE_TIME filter with GTE operator when responseTime is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...component.moreFiltersValues(), responseTime: 300 });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'RESPONSE_TIME', operator: 'GTE', value: 300 });
    });

    // NEW: covers the responseTime == null branch
    it('should not include a RESPONSE_TIME filter when responseTime is null', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...component.moreFiltersValues(), responseTime: null });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters?.find((f: any) => f.name === 'RESPONSE_TIME')).toBeUndefined();
    });

    it('should use explicit from/to dates when both are set, ignoring the period', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      const from = moment('2026-01-01T00:00:00');
      const to = moment('2026-01-02T00:00:00');
      component.moreFiltersValues.set({ ...component.moreFiltersValues(), from, to });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.timeRange?.from).toBe(from.toISOString());
      expect(last?.timeRange?.to).toBe(to.toISOString());
    });

    it('should compute timeRange from the selected period when no from/to is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      const before = Date.now();
      component.form.patchValue({ period: { label: 'Last 1 Hour', value: '-1h' } });
      fixture.detectChanges();
      const after = Date.now();

      const last: any = emitted[emitted.length - 1];
      const fromMs = new Date(last?.timeRange?.from).getTime();
      const toMs = new Date(last?.timeRange?.to).getTime();
      expect(toMs).toBeGreaterThanOrEqual(before);
      expect(toMs).toBeLessThanOrEqual(after + 10);
      expect(toMs - fromMs).toBeCloseTo(60 * 60 * 1000, -3);
    });

    it('should emit no timeRange when period is "0" (None) and no explicit dates', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.form.patchValue({ period: { label: 'None', value: '0' } });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.timeRange).toBeUndefined();
    });

    it('should fall back to a 24h window for an unrecognised period value', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      const before = Date.now();
      component.form.patchValue({ period: { label: 'Unknown', value: '-99x' } as any });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      const fromMs = new Date(last?.timeRange?.from).getTime();
      const toMs = new Date(last?.timeRange?.to).getTime();
      expect(toMs).toBeGreaterThanOrEqual(before);
      expect(toMs - fromMs).toBeCloseTo(24 * 60 * 60 * 1000, -4);
    });

    it('should include an APPLICATION filter when applications are selected', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.form.patchValue({ applications: ['app-1', 'app-2'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'APPLICATION', operator: 'IN', value: ['app-1', 'app-2'] });
    });

    it('should include an ENTRYPOINT filter when entrypoints are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, entrypoints: ['http-proxy', 'sse'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'ENTRYPOINT', operator: 'IN', value: ['http-proxy', 'sse'] });
    });

    it('should include an HTTP_METHOD filter when methods are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, methods: ['GET', 'POST'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'HTTP_METHOD', operator: 'IN', value: ['GET', 'POST'] });
    });

    it('should include a PLAN filter when plans are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, plans: ['plan-gold'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'PLAN', operator: 'IN', value: ['plan-gold'] });
    });

    it('should include an HTTP_STATUS filter when statuses are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, statuses: new Set([200, 500]) });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      const statusFilter = last?.filters?.find((f: any) => f.name === 'HTTP_STATUS');
      expect(statusFilter?.operator).toBe('IN');
      expect((statusFilter?.value as number[]).sort()).toEqual([200, 500]);
    });

    it('should include a MCP_METHOD filter when mcpMethod is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, mcpMethod: 'tools/call' });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'MCP_METHOD', operator: 'EQ', value: 'tools/call' });
    });

    it('should include a TRANSACTION_ID filter when transactionId is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, transactionId: 'tx-abc-123' });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'TRANSACTION_ID', operator: 'EQ', value: 'tx-abc-123' });
    });

    it('should include a REQUEST_ID filter when requestId is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, requestId: 'req-xyz-456' });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'REQUEST_ID', operator: 'EQ', value: 'req-xyz-456' });
    });

    it('should include a URI filter when uri is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, uri: '/api/v1/users' });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.filters).toContainEqual({ name: 'URI', operator: 'EQ', value: '/api/v1/users' });
    });
  });
  it('should compute filtersParam with API filter', () => {
    component.form.patchValue({ apis: ['api-1'] });

    const result = component.filtersParam();

    expect(result.filters).toContainEqual({
      name: 'API',
      operator: 'IN',
      value: ['api-1'],
    });
  });
  it('should resolve time range for -1h period', () => {
    const more = component.moreFiltersValues();

    const range = (component as any).resolveTimeRange('-1h', more);

    expect(range).toBeDefined();
  });
});
