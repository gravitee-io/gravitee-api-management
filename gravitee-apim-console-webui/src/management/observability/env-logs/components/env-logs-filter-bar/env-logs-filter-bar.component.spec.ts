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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatChipHarness, MatChipSetHarness } from '@angular/material/chips/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import moment from 'moment';

import { EnvLogsFilterBarComponent } from './env-logs-filter-bar.component';

import { ENV_LOGS_DEFAULT_PERIOD } from '../../models/env-logs-more-filters.model';
import { GioTestingModule } from '../../../../../shared/testing';

describe('EnvLogsFilterBarComponent', () => {
  let component: EnvLogsFilterBarComponent;
  let fixture: ComponentFixture<EnvLogsFilterBarComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, EnvLogsFilterBarComponent],
      providers: [provideNoopAnimations()],
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
  });
});
