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
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Observable, of } from 'rxjs';

import { GioSelectSearchComponent, SelectOption, ResultsLoaderInput, ResultsLoaderOutput } from './gio-select-search.component';
import { GioSelectSearchHarness } from './gio-select-search.harness';

import { GioTestingModule } from '../../testing';

@Component({
  template: `
    <form [formGroup]="form">
      @if (resultsLoader) {
        <gio-select-search
          [options]="options"
          [label]="label"
          [placeholder]="placeholder"
          [resultsLoader]="resultsLoader"
          formControlName="selection"
        />
      } @else {
        <gio-select-search [options]="options" [label]="label" [placeholder]="placeholder" formControlName="selection" />
      }
    </form>
  `,
  imports: [ReactiveFormsModule, GioSelectSearchComponent],
  standalone: true,
})
class TestComponent {
  form: FormGroup;
  options: SelectOption[] = [];
  label = 'Test Label';
  placeholder = 'Search options...';
  resultsLoader: (data: ResultsLoaderInput) => Observable<ResultsLoaderOutput>;

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      selection: [[]],
    });
  }
}

describe('GioSelectSearchComponent', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let mockNewResults: jest.Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  });

  describe('Basic functionality', () => {
    it('should show no options if no options are passed into the component', async () => {
      component.options = [];
      fixture.detectChanges();

      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      const options = await harness.getFilteredOptionLabels();
      expect(options.length).toBe(1); // Only the "No options found" option

      expect(options[0]).toContain('No options available');
    });

    it('should show options when options are provided', async () => {
      component.options = [
        { value: 'option1', label: 'Option 1' },
        { value: 'option2', label: 'Option 2' },
        { value: 'option3', label: 'Option 3' },
      ];
      fixture.detectChanges();

      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      const optionTexts = await harness.getFilteredOptionLabels();
      expect(optionTexts).toEqual(['Option 1', 'Option 2', 'Option 3']);
    });
  });

  describe('Multiple selection', () => {
    beforeEach(() => {
      component.options = [
        { value: 'option1', label: 'Option 1' },
        { value: 'option2', label: 'Option 2' },
        { value: 'option3', label: 'Option 3' },
        { value: 'option4', label: 'Option 4' },
      ];
      fixture.detectChanges();
    });

    it('should allow user to select multiple options', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();
      fixture.detectChanges();

      // Select first option
      await harness.checkOptionByLabel('Option 1');
      fixture.detectChanges();

      // Select second option
      await harness.checkOptionByLabel('Option 2');
      fixture.detectChanges();

      const selectedValues = component.form.get('selection')?.value;
      expect(selectedValues).toEqual(['option1', 'option2']);
    });

    it('should show correct trigger label when multiple options are selected', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Select multiple options
      await harness.checkOptionByLabel('Option 1');
      await harness.checkOptionByLabel('Option 2');
      await harness.checkOptionByLabel('Option 3');

      const triggerText = await harness.getTriggerText();
      expect(triggerText).toContain('Test Label');
      expect(triggerText).toContain('3'); // Should show count of selected items
    });

    it('should clear all selections when clear selection button is clicked', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Select multiple options
      await harness.checkOptionByLabel('Option 1');
      await harness.checkOptionByLabel('Option 2');
      await harness.checkOptionByLabel('Option 3');
      fixture.detectChanges();

      // Verify selections are made
      let formValue = component.form.get('selection')?.value;
      expect(formValue).toEqual(['option1', 'option2', 'option3']);

      // Click the clear selection button
      await harness.clearSelection();

      // Verify all selections are cleared
      formValue = component.form.get('selection')?.value;
      expect(formValue).toEqual([]);

      // Verify trigger text no longer shows count
      const triggerText = await harness.getTriggerText();
      expect(triggerText).toContain('Test Label');
      expect(triggerText).not.toContain('3'); // Should not show count anymore
    });
  });

  describe('Search functionality', () => {
    beforeEach(() => {
      component.options = [
        { value: 'apple', label: 'Apple' },
        { value: 'banana', label: 'Banana' },
        { value: 'cherry', label: 'Cherry' },
        { value: 'date', label: 'Date' },
        { value: 'elderberry', label: 'Elderberry' },
      ];
      fixture.detectChanges();
    });

    it('should filter options when user searches in the drop-down', async () => {
      const harness = await rootLoader.getHarness(GioSelectSearchHarness);

      // First, verify we can open the dropdown
      await harness.open();

      // Get all options first to verify the dropdown is working
      const allOptions = await harness.getFilteredOptionLabels();
      expect(allOptions.length).toBe(5); // All 5 options should be visible

      await harness.setSearchValue('a');
      fixture.detectChanges();

      const filteredOptions = await harness.getFilteredOptionLabels();

      // Should only show options containing 'a'
      expect(filteredOptions).toEqual(['Apple', 'Banana', 'Date']);
    });

    it('should allow user to select multiple options after searching', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Search for 'a'
      await harness.setSearchValue('a');
      fixture.detectChanges();

      const options = await harness.getFilteredOptionLabels();
      expect(options).toEqual(['Apple', 'Banana', 'Date']);

      // Select filtered options
      await harness.checkOptionByLabel('Apple'); // Apple
      await harness.checkOptionByLabel('Banana'); // Banana

      const selectedValues = component.form.get('selection')?.value;
      expect(selectedValues).toEqual(['apple', 'banana']);
    });

    it('should update trigger label to show multiple selected options after search', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();
      // Search for 'a' and select multiple options
      await harness.setSearchValue('a');
      fixture.detectChanges();

      // Select all filtered options
      await harness.checkOptionByLabel('Date');
      await harness.checkOptionByLabel('Apple');
      await harness.checkOptionByLabel('Banana');

      const triggerText = await harness.getTriggerText();
      expect(triggerText).toContain('Test Label');
      expect(triggerText).toContain('3'); // Should show count of selected items
    });

    it('should clear search when clear button is clicked', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();
      // Search for something
      await harness.setSearchValue('a');
      fixture.detectChanges();

      const filteredOptions = await harness.getFilteredOptionLabels();
      expect(filteredOptions).toHaveLength(3); // Should show 3 options
      // Clear search
      await harness.clearSearch();
      fixture.detectChanges();

      const optionsAfterEmptySearch = await harness.getFilteredOptionLabels();
      expect(optionsAfterEmptySearch).toHaveLength(5); // All original options should be back
    });

    it('should have search input in the dropdown overlay', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness);

      // Open dropdown
      await harness.open();

      // Try to find the search input
      const searchValue = await harness.getSearchValue();
      expect(searchValue).toBeDefined();
    });

    it('should reset search when overlay is closed and reopened', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness);

      // Open dropdown
      await harness.open();

      // Verify all options are initially shown
      let options = await harness.getFilteredOptionLabels();
      expect(options).toHaveLength(5); // All 5 options should be visible

      // Search for 'a' to filter options
      await harness.setSearchValue('a');
      fixture.detectChanges();

      // Verify filtered options are shown
      const filteredOptions = await harness.getFilteredOptionLabels();
      expect(filteredOptions).toEqual(['Apple', 'Banana', 'Date']);

      // Close the overlay
      await harness.close();
      fixture.detectChanges();

      // Reopen the overlay
      await harness.open();
      fixture.detectChanges();

      // Verify search is cleared and all options are shown again
      const searchValue = await harness.getSearchValue();
      expect(searchValue).toBe(''); // Search should be empty

      options = await harness.getFilteredOptionLabels();
      expect(options).toHaveLength(5); // All options should be visible again
    });
  });

  describe('Form integration', () => {
    beforeEach(() => {
      component.options = [
        { value: 'option1', label: 'Option 1' },
        { value: 'option2', label: 'Option 2' },
        { value: 'option3', label: 'Option 3' },
      ];
      fixture.detectChanges();
    });

    it('should update form control value when options are selected', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness.with({ formControlName: 'selection' }));
      await harness.open();

      await harness.checkOptionByLabel('Option 1');
      await harness.checkOptionByLabel('Option 2');
      fixture.detectChanges();

      const formValue = component.form.get('selection')?.value;
      expect(formValue).toEqual(['option1', 'option2']);
    });

    it('should reflect form control value in component', async () => {
      // Set form value programmatically
      component.form.get('selection')?.setValue(['option1', 'option3']);
      fixture.detectChanges();

      const harness = await loader.getHarness(GioSelectSearchHarness.with({ formControlName: 'selection' }));
      await harness.open();

      const selectedValues = await harness.getSelectedValues();
      expect(selectedValues).toEqual(['option1', 'option3']);
    });

    it('should be able to get harness by label', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness.with({ label: 'Test Label' }));
      await harness.open();

      const triggerText = await harness.getTriggerText();
      expect(triggerText).toContain('Test Label');
    });

    it('should be able to get harness by placeholder', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness.with({ placeholder: 'Search options...' }));
      await harness.open();

      const placeholder = await harness.getPlaceholder();
      expect(placeholder).toBe('Search options...');
    });

    it('should be able to get harness by formControlName', async () => {
      const harness = await loader.getHarness(GioSelectSearchHarness.with({ formControlName: 'selection' }));
      await harness.open();

      const isOpen = await harness.isOpen();
      expect(isOpen).toBe(true);
    });
  });

  describe('Internal pagination functionality', () => {
    beforeEach(async () => {
      mockNewResults = jest.fn();
      component.resultsLoader = (data: any) => mockNewResults(data);
    });

    it('should have newResults$ properly set up', () => {
      expect(component.resultsLoader).toBeDefined();
      expect(typeof component.resultsLoader).toBe('function');
    });

    it('should not call backend when component is closed', async () => {
      // Mock the newResults function to return an observable
      mockNewResults.mockReturnValue(of({ data: [], hasNextPage: false }));

      // Verify no backend calls were made
      expect(mockNewResults).not.toHaveBeenCalled();
    });

    it('should request first page with empty search term when opened', async () => {
      // Mock the newResults function to return an observable
      mockNewResults.mockReturnValue(of({ data: [], hasNextPage: false }));

      // Open the overlay
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Verify the first page was requested with empty search term
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: '', page: 0 });
      expect(mockNewResults).toHaveBeenCalledTimes(1);
    });

    it('should load next page and accumulate results when scrolled', async () => {
      // Mock the newResults function to return different data for each page
      mockNewResults
        .mockReturnValueOnce(of({ data: [{ value: '1', label: 'Option 1' }], hasNextPage: true }))
        .mockReturnValueOnce(of({ data: [{ value: '2', label: 'Option 2' }], hasNextPage: false }));

      // Open the overlay
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Verify first page was loaded
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: '', page: 0 });

      // Simulate scrolling to load more
      await harness.scrollNearBottom();

      // // Verify second page was requested
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: '', page: 1 });

      // Verify total calls
      expect(mockNewResults).toHaveBeenCalledTimes(2);
    });

    it('should not call for new page when reaching last page', async () => {
      // Mock the newResults function to return data with no next page
      mockNewResults.mockReturnValue(of({ data: [{ value: '1', label: 'Option 1' }], hasNextPage: false }));

      // Open the overlay
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Verify first page was loaded
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: '', page: 0 });

      // Simulate scrolling multiple times
      await harness.scrollNearBottom();
      await harness.scrollNearBottom();
      await harness.scrollNearBottom();

      // Verify only the first page was requested (no more pages available)
      expect(mockNewResults).toHaveBeenCalledTimes(1);
    });

    it('should request first page with search term when searching', async () => {
      // Mock the newResults function to return an observable
      mockNewResults.mockReturnValue(of({ data: [], hasNextPage: false }));

      // Open the overlay
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Search for something
      await harness.setSearchValue('test');

      // Verify search was requested with the search term
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: 'test', page: 0 });
    });

    it('should request first page with empty search term when clearing search', async () => {
      // Mock the newResults function to return an observable
      mockNewResults.mockReturnValue(of({ data: [], hasNextPage: false }));

      // Open the overlay
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Clear the initial call
      mockNewResults.mockClear();

      // Search for something first
      await harness.setSearchValue('test');

      // Clear the search
      await harness.clearSearch();

      // Verify search was requested with empty search term
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: '', page: 0 });
    });

    it('should accumulate results from multiple pages correctly', async () => {
      // Mock the newResults function to return different data for each page
      mockNewResults
        .mockReturnValueOnce(
          of({
            data: [
              { value: '1', label: 'Option 1' },
              { value: '2', label: 'Option 2' },
            ],
            hasNextPage: true,
          }),
        )
        .mockReturnValueOnce(
          of({
            data: [
              { value: '3', label: 'Option 3' },
              { value: '4', label: 'Option 4' },
            ],
            hasNextPage: false,
          }),
        );

      // Open the overlay
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Verify first page was loaded
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: '', page: 0 });

      // Simulate scrolling to load more
      await harness.scrollNearBottom();

      // Verify second page was requested
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: '', page: 1 });

      // Verify total calls
      expect(mockNewResults).toHaveBeenCalledTimes(2);
    });

    it('should handle search and pagination together', async () => {
      // Mock the newResults function to return different data for search and pagination
      mockNewResults
        .mockReturnValueOnce(of({ data: [{ value: '0', label: 'Empty search term result' }], hasNextPage: false })) // Initial empty search
        .mockReturnValueOnce(
          of({
            data: [
              { value: '1', label: 'Search Result 1' },
              { value: '2', label: 'Search Result 2' },
            ],
            hasNextPage: true,
          }),
        )
        .mockReturnValueOnce(
          of({
            data: [{ value: '3', label: 'Search Result 3' }],
            hasNextPage: false,
          }),
        );

      // Open the overlay
      const harness = await loader.getHarness(GioSelectSearchHarness);
      await harness.open();

      // Search for something
      await harness.setSearchValue('search');

      // Verify search was requested
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: 'search', page: 0 });

      // Simulate scrolling to load more
      await harness.scrollNearBottom();

      // Verify next page was requested with same search term
      expect(mockNewResults).toHaveBeenCalledWith({ searchTerm: 'search', page: 1 });

      // Verify total calls
      expect(mockNewResults).toHaveBeenCalledTimes(3);
    });
  });
});
