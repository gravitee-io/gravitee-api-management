/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Observable, of } from 'rxjs';

import { DropdownSearchComponent, ResultsLoaderOutput, SelectOption } from './dropdown-search.component';
import { DropdownSearchComponentHarness } from './dropdown-search.component.harness';

const mockOptions: SelectOption[] = [
  { value: 'opt1', label: 'Option 1' },
  { value: 'opt2', label: 'Option 2' },
  { value: 'opt3', label: 'Option 3' },
];

describe('DropdownSearchComponent', () => {
  @Component({
    standalone: true,
    imports: [DropdownSearchComponent, ReactiveFormsModule],
    template: `
      <app-dropdown-search
        [label]="label"
        [options]="options"
        [formControl]="control"
        [resultsLoader]="resultsLoader"
        [placeholder]="placeholder"
      />
    `,
  })
  class TestComponent {
    label = 'Test Label';
    options: SelectOption[] = mockOptions;
    control = new FormControl<string[]>([]);
    placeholder = 'Search...';
    resultsLoader: (params: { searchTerm: string; page: number }) => Observable<ResultsLoaderOutput>;

    constructor() {
      this.resultsLoader = ({ searchTerm }: { searchTerm: string; page: number }) => {
        const data = searchTerm
          ? this.options.filter(option => option.label.toLowerCase().includes(searchTerm.toLowerCase()))
          : this.options;
        return of({ data, hasNextPage: false });
      };
    }
  }

  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let component: TestComponent;
  let harness: DropdownSearchComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(DropdownSearchComponentHarness);
    fixture.detectChanges();
  });

  it('should create', async () => {
    expect(harness).toBeTruthy();
    expect(await harness.getTriggerText()).toContain('Test Label');
  });

  describe('Form Integration', () => {
    it('should reflect initial form value', async () => {
      component.control.setValue(['opt1', 'opt2']);
      fixture.detectChanges();
      expect(await harness.getTriggerText()).toContain('2');
      const checked = await harness.getCheckedOptions();
      expect(checked.length).toBe(2);
    });

    it('should update form value when options are selected', async () => {
      await harness.toggleOption('Option 1');
      expect(component.control.value).toEqual(['opt1']);
      expect(await harness.getTriggerText()).toContain('1');

      await harness.toggleOption('Option 2');
      expect(component.control.value).toEqual(['opt1', 'opt2']);
      expect(await harness.getTriggerText()).toContain('2');
    });

    it('should update form value when options are deselected', async () => {
      component.control.setValue(['opt1', 'opt2']);
      fixture.detectChanges();

      await harness.toggleOption('Option 1');
      expect(component.control.value).toEqual(['opt2']);
      expect(await harness.getTriggerText()).toContain('1');
    });

    it('should clear selection via clear button', async () => {
      component.control.setValue(['opt1', 'opt2']);
      fixture.detectChanges();

      await harness.clearSelection();
      expect(component.control.value).toEqual([]);
      expect(await harness.getTriggerText()).not.toContain('(');
    });

    it('should handle disabled state', async () => {
      component.control.disable();
      fixture.detectChanges();

      expect(await harness.isDisabled()).toBe(true);
      await harness.open();
      expect(await harness.isOpen()).toBe(false);

      component.control.enable();
      fixture.detectChanges();
      expect(await harness.isDisabled()).toBe(false);
    });

    it('should handle form reset', async () => {
      component.control.setValue(['opt1']);
      fixture.detectChanges();

      component.control.reset([]);
      fixture.detectChanges();

      expect(await harness.getTriggerText()).not.toContain('1');
      const checked = await harness.getCheckedOptions();
      expect(checked.length).toBe(0);
    });

    it('should be touched and dirty after interaction', async () => {
      expect(component.control.touched).toBe(false);
      expect(component.control.dirty).toBe(false);

      await harness.toggleOption('Option 1');

      expect(component.control.touched).toBe(true);
      expect(component.control.dirty).toBe(true);
    });

    it('should validate correctly with Validators.required', async () => {
      component.control.setValidators(Validators.required);
      component.control.updateValueAndValidity();
      fixture.detectChanges();

      expect(component.control.valid).toBe(false);

      await harness.toggleOption('Option 1');
      expect(component.control.valid).toBe(true);

      await harness.toggleOption('Option 1'); // Deselect
      expect(component.control.valid).toBe(false);
    });
  });

  describe('Search and Loading', () => {
    const MOCK_DATE = new Date(1466424490000);

    it('should filter options based on search term (default)', async () => {
      jest.useRealTimers();
      try {
        await harness.open();
        fixture.detectChanges();
        await harness.setSearchTerm('Option 1');
        fixture.detectChanges();
        await new Promise(resolve => setTimeout(resolve, 350));
        await fixture.whenStable();
        fixture.detectChanges();
        const labels = await harness.getOptionsLabels();
        expect(labels).toEqual(['Option 1']);
      } finally {
        jest.useFakeTimers({ advanceTimers: 1, now: MOCK_DATE });
      }
    });

    it('should use custom resultsLoader', async () => {
      const customOptions: SelectOption[] = [{ value: 'custom', label: 'Custom Option' }];
      const resultsLoader = jest.fn().mockReturnValue(
        of({
          data: customOptions,
          hasNextPage: false,
        } as ResultsLoaderOutput),
      );
      component.resultsLoader = resultsLoader;
      fixture.detectChanges();

      await harness.open();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(resultsLoader).toHaveBeenCalled();
      const labels = await harness.getOptionsLabels();
      expect(labels).toContain('Custom Option');
    });

    it('should show "No Results Found" when no options match', async () => {
      jest.useRealTimers();
      try {
        await harness.open();
        fixture.detectChanges();
        await harness.setSearchTerm('Non-existent');
        fixture.detectChanges();
        await new Promise(resolve => setTimeout(resolve, 350));
        await fixture.whenStable();
        fixture.detectChanges();
        const labels = await harness.getOptionsLabels();
        expect(labels).toEqual(['No Results Found']);
      } finally {
        jest.useFakeTimers({ advanceTimers: 1, now: MOCK_DATE });
      }
    });
  });

  describe('Overlay Management', () => {
    it('should open and close overlay', async () => {
      expect(await harness.isOpen()).toBe(false);
      await harness.open();
      expect(await harness.isOpen()).toBe(true);
      await harness.close();
      expect(await harness.isOpen()).toBe(false);
    });
  });
});
