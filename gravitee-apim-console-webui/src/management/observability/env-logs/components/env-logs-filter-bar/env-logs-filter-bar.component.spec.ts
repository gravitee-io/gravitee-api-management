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

  it('should display the period dropdown and the two gio-select-search components', async () => {
    const selects = await loader.getAllHarnesses(MatSelectHarness);
    expect(selects.length).toBe(1); // Only Period

    const apisSearch = fixture.nativeElement.querySelector('[data-testid="apis-select"]');
    const appsSearch = fixture.nativeElement.querySelector('[data-testid="applications-select"]');
    expect(apisSearch).toBeTruthy();
    expect(appsSearch).toBeTruthy();
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
    it('should display a chip when an API is selected via form control', async () => {
      // Simulate selecting an API via the reactive form
      component.form.patchValue({ apis: ['api-1'] });
      // Cache the label for chip display
      component.onApiOptionsLoaded([{ value: 'api-1', label: 'Weather API' }]);
      fixture.detectChanges();
      await fixture.whenStable();

      const chipSet = await loader.getHarness(MatChipSetHarness);
      const chips = await chipSet.getChips();
      expect(chips.length).toBe(1);
    });

    it('should display a chip when an Application is selected via form control', async () => {
      component.form.patchValue({ applications: ['app-1'] });
      component.onAppOptionsLoaded([{ value: 'app-1', label: 'Mobile App' }]);
      fixture.detectChanges();
      await fixture.whenStable();

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
      // Select an API via form control
      component.form.patchValue({ apis: ['api-1'] });
      component.onApiOptionsLoaded([{ value: 'api-1', label: 'Weather API' }]);
      fixture.detectChanges();
      await fixture.whenStable();

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
      // Select an API via form control
      component.form.patchValue({ apis: ['api-1'] });
      component.onApiOptionsLoaded([{ value: 'api-1', label: 'Weather API' }]);
      fixture.detectChanges();
      await fixture.whenStable();

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

    it('should remove a scalar more-filter chip (e.g. transactionId) from the moreFiltersValues signal', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        transactionId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
      });
      fixture.detectChanges();

      component.removeChip({
        key: 'transactionId',
        value: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        display: 'Transaction ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890',
      });

      expect(component.moreFiltersValues().transactionId).toBeNull();
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

      component.removeChip({ key: 'responseTime', value: 500, display: 'Response time: >=500ms' });

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
      expect(rtChip!.display).toBe('Response time: >=250ms');
    });

    it('should display the API name (not id) in the chip', () => {
      component.onApiOptionsLoaded([{ value: 'api-1', label: 'Weather API' }]);
      component.form.patchValue({ apis: ['api-1'] });
      fixture.detectChanges();

      const chip = component.filterChips().find(c => c.key === 'apis');
      expect(chip?.display).toBe('Weather API');
      expect(chip?.value).toBe('api-1');
    });

    it('should produce chips for errorKeys', () => {
      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, errorKeys: ['TIMEOUT'] });
      fixture.detectChanges();

      const chip = component.filterChips().find(c => c.key === 'errorKeys');
      expect(chip).toBeDefined();
      expect(chip?.value).toBe('TIMEOUT');
      expect(chip?.display).toBe('TIMEOUT');
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
        statuses: new Set([200]),
      };

      component.applyMoreFilters(newValues);

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
        entrypoints: ['http-proxy'],
      });
      fixture.detectChanges();

      component.resetAllFilters();

      expect(component.form.value.apis).toBeNull();
      expect(component.moreFiltersValues().statuses.size).toBe(0);
      expect(component.moreFiltersValues().entrypoints).toBeNull();
    });

    it('should reset errorKeys when resetAllFilters is called', () => {
      component.moreFiltersValues.set({ ...component.moreFiltersValues(), errorKeys: ['TIMEOUT'] });
      fixture.detectChanges();

      component.resetAllFilters();

      expect(component.moreFiltersValues().errorKeys).toBeNull();
    });
  });

  describe('filtersChanged output — filter values', () => {
    it('should emit errorKeys in more when errorKeys are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        errorKeys: ['TIMEOUT', 'ASSIGN_CONTENT_ERROR'],
      });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.errorKeys).toEqual(['TIMEOUT', 'ASSIGN_CONTENT_ERROR']);
    });

    it('should emit no errorKeys when errorKeys is empty', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...component.moreFiltersValues(), errorKeys: [] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.errorKeys).toEqual([]);
    });

    it('should emit null errorKeys when errorKeys is null', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...component.moreFiltersValues(), errorKeys: null });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.errorKeys).toBeNull();
    });

    it('should emit responseTime in more when responseTime is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...component.moreFiltersValues(), responseTime: 300 });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.responseTime).toBe(300);
    });

    it('should emit null responseTime when responseTime is null', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...component.moreFiltersValues(), responseTime: null });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.responseTime).toBeNull();
    });

    it('should emit from/to dates in more when both are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      const from = moment('2026-01-01T00:00:00');
      const to = moment('2026-01-02T00:00:00');
      component.moreFiltersValues.set({ ...component.moreFiltersValues(), from, to });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.from?.toISOString()).toBe(from.toISOString());
      expect(last?.more?.to?.toISOString()).toBe(to.toISOString());
    });

    it('should emit the selected period value', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.form.patchValue({ period: { label: 'Last 1 Hour', value: '-1h' } });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.period).toBe('-1h');
    });

    it('should emit period "0" when None is selected', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.form.patchValue({ period: { label: 'None', value: '0' } });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.period).toBe('0');
    });

    it('should emit applicationIds when applications are selected', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.form.patchValue({ applications: ['app-1', 'app-2'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.applicationIds).toEqual(['app-1', 'app-2']);
    });

    it('should emit entrypoints in more when entrypoints are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, entrypoints: ['http-proxy', 'sse'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.entrypoints).toEqual(['http-proxy', 'sse']);
    });

    it('should emit methods in more when methods are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, methods: ['GET', 'POST'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.methods).toEqual(['GET', 'POST']);
    });

    it('should emit plans in more when plans are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, plans: ['plan-gold'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.plans).toEqual(['plan-gold']);
    });

    it('should emit statuses in more when statuses are set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, statuses: new Set([200, 500]) });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.statuses).toEqual(new Set([200, 500]));
    });

    it('should emit transactionId in more when transactionId is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, transactionId: 'tx-abc-123' });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.transactionId).toBe('tx-abc-123');
    });

    it('should emit requestId in more when requestId is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, requestId: 'req-xyz-456' });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.requestId).toBe('req-xyz-456');
    });

    it('should emit uri in more when uri is set', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.moreFiltersValues.set({ ...DEFAULT_MORE_FILTERS, uri: '/api/v1/users' });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.more?.uri).toBe('/api/v1/users');
    });

    it('should emit apiIds when APIs are selected', () => {
      const emitted: unknown[] = [];
      component.filtersChanged.subscribe(v => emitted.push(v));

      component.form.patchValue({ apis: ['api-1'] });
      fixture.detectChanges();

      const last: any = emitted[emitted.length - 1];
      expect(last?.apiIds).toEqual(['api-1']);
    });
  });

  describe('filtersChanged output', () => {
    it('should emit filter values when form changes', async () => {
      const spy = jest.fn();
      component.filtersChanged.subscribe(spy);

      component.form.patchValue({ apis: ['api-1'] });
      fixture.detectChanges();
      await fixture.whenStable();

      expect(spy).toHaveBeenCalled();
      const lastCall = spy.mock.calls[spy.mock.calls.length - 1][0];
      expect(lastCall.apiIds).toEqual(['api-1']);
    });
  });

  describe('removeChip for period', () => {
    it('should reset period to default when period chip is removed', () => {
      const customPeriod = { label: 'Last 1 Hour', value: '-1h' };
      component.form.controls.period.setValue(customPeriod);
      fixture.detectChanges();

      component.removeChip({ key: 'period', value: '-1h', display: 'Last 1 Hour' });

      expect(component.form.controls.period.value).toEqual(ENV_LOGS_DEFAULT_PERIOD);
    });
  });

  describe('removeChip for quick-filter controls', () => {
    it('should remove an API from the apis form control and set to null when last is removed', () => {
      component.form.patchValue({ apis: ['api-1', 'api-2'] });
      component.onApiOptionsLoaded([
        { value: 'api-1', label: 'API One' },
        { value: 'api-2', label: 'API Two' },
      ]);
      fixture.detectChanges();

      component.removeChip({ key: 'apis', value: 'api-1', display: 'API One' });
      expect(component.form.controls.apis.value).toEqual(['api-2']);

      component.removeChip({ key: 'apis', value: 'api-2', display: 'API Two' });
      expect(component.form.controls.apis.value).toBeNull();
    });

    it('should remove an Application from the applications form control', () => {
      component.form.patchValue({ applications: ['app-1'] });
      component.onAppOptionsLoaded([{ value: 'app-1', label: 'My App' }]);
      fixture.detectChanges();

      component.removeChip({ key: 'applications', value: 'app-1', display: 'My App' });
      expect(component.form.controls.applications.value).toBeNull();
    });
  });

  describe('filterChips computed signal — additional coverage', () => {
    it('should include status chips when statuses are set', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        statuses: new Set([200, 500]),
      });
      fixture.detectChanges();

      const chips = component.filterChips();
      const statusChips = chips.filter(c => c.key === 'statuses');
      expect(statusChips.length).toBe(2);
      expect(statusChips.map(c => c.value)).toEqual(expect.arrayContaining([200, 500]));
    });

    it('should include string filter chips (transactionId, requestId, uri)', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        transactionId: 'tx-123',
        requestId: 'req-456',
        uri: '/api/v1/test',
      });
      fixture.detectChanges();

      const chips = component.filterChips();
      expect(chips.find(c => c.key === 'transactionId')?.display).toBe('Transaction ID: tx-123');
      expect(chips.find(c => c.key === 'requestId')?.display).toBe('Request ID: req-456');
      expect(chips.find(c => c.key === 'uri')?.display).toBe('URI: /api/v1/test');
    });

    it('should include array filter chips (entrypoints, methods, plans)', () => {
      component.moreFiltersValues.set({
        ...component.moreFiltersValues(),
        entrypoints: ['http-proxy'],
        methods: ['GET', 'POST'],
        plans: ['plan-1'],
      });
      fixture.detectChanges();

      const chips = component.filterChips();
      expect(chips.filter(c => c.key === 'entrypoints').length).toBe(1);
      expect(chips.filter(c => c.key === 'methods').length).toBe(2);
      expect(chips.filter(c => c.key === 'plans').length).toBe(1);
    });
  });
});
