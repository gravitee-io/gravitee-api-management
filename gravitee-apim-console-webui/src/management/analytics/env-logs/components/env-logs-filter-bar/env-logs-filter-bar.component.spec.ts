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

import { EnvLogsFilterBarComponent, ENV_LOGS_DEFAULT_PERIOD } from './env-logs-filter-bar.component';

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

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display all filter dropdowns', async () => {
    const selects = await loader.getAllHarnesses(MatSelectHarness);
    expect(selects.length).toBe(4); // Period, Entrypoints, HTTP methods, Plan
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
    it('should display a chip when a method is selected', async () => {
      const methodsSelect = await loader.getHarness(MatSelectHarness.with({ ancestor: '[data-testid="methods-select"]' }));
      await methodsSelect.open();
      const [getOption] = await methodsSelect.getOptions({ text: 'GET' });
      await getOption.click();

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
      // Select a method first
      const methodsSelect = await loader.getHarness(MatSelectHarness.with({ ancestor: '[data-testid="methods-select"]' }));
      await methodsSelect.open();
      const [getOption] = await methodsSelect.getOptions({ text: 'GET' });
      await getOption.click();

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
      // Select a method first
      const methodsSelect = await loader.getHarness(MatSelectHarness.with({ ancestor: '[data-testid="methods-select"]' }));
      await methodsSelect.open();
      const [getOption] = await methodsSelect.getOptions({ text: 'GET' });
      await getOption.click();

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
});
