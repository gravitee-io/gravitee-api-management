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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

export interface GioSelectSearchHarnessFilters extends BaseHarnessFilters {
  label?: string;
  placeholder?: string;
  selector?: string;
  formControlName?: string;
}

export class GioSelectSearchHarness extends ComponentHarness {
  static readonly hostSelector = 'gio-select-search';

  static with(options: GioSelectSearchHarnessFilters = {}): HarnessPredicate<GioSelectSearchHarness> {
    return new HarnessPredicate(GioSelectSearchHarness, options).addOption(
      'formControlName',
      options.formControlName,
      async (harness, formControlName) => {
        return HarnessPredicate.stringMatches(harness.getFormControlName(), formControlName);
      },
    );
  }

  private readonly _documentRootLocator = this.documentRootLocatorFactory();

  // Trigger element
  protected getTrigger = this.locatorFor('.gio-select-search__trigger');

  // Overlay elements (when open)
  protected getOverlay = this._documentRootLocator.locatorForOptional('.gio-select-search-overlay');
  protected getSearchInput = this._documentRootLocator.locatorForOptional(
    MatInputHarness.with({ selector: '.gio-select-search__search-field input' }),
  );
  protected getClearButton = this._documentRootLocator.locatorForOptional(
    MatButtonHarness.with({ selector: '[aria-label="Clear search"]' }),
  );
  protected getClearSelectionButton = this._documentRootLocator.locatorForOptional(
    MatButtonHarness.with({ selector: '[aria-label="Clear selection"]' }),
  );
  protected getOptions = this._documentRootLocator.locatorForAll('.gio-select-search__option');
  protected getOptionLabels = this._documentRootLocator.locatorForAll('.gio-select-search__option-label');
  protected getCheckboxes = this._documentRootLocator.locatorForAll(MatCheckboxHarness);
  protected getCheckedCheckboxes = this._documentRootLocator.locatorForAll(MatCheckboxHarness.with({ checked: true }));

  /**
   * Opens the select dropdown
   */
  async open(): Promise<void> {
    const trigger = await this.getTrigger();
    await trigger.click();
  }

  /**
   * Closes the select dropdown
   */
  async close(): Promise<void> {
    await this._documentRootLocator
      .locatorForOptional('.cdk-overlay-backdrop')()
      .then(res => res.click());
  }

  /**
   * Gets the current trigger text (label and count)
   */
  async getTriggerText(): Promise<string> {
    const trigger = await this.getTrigger();
    return trigger.text();
  }

  /**
   * Gets the text of all visible options
   */
  async getFilteredOptionLabels(): Promise<string[]> {
    const options = await this.getOptionLabels();
    const texts: string[] = [];

    for (const option of options) {
      texts.push(await option.text());
    }

    return texts;
  }

  /**
   * Gets the currently selected values
   */
  async getSelectedValues(): Promise<string[]> {
    const checkboxes = await this.getCheckedCheckboxes();
    const selectedValues: string[] = [];

    for (const checkbox of checkboxes) {
      selectedValues.push(await checkbox.getValue());
    }

    return selectedValues;
  }

  async checkOptionByLabel(label: string): Promise<void> {
    const options = await this.getOptions();
    for (const option of options) {
      const optionLabel = await option.text();
      if (optionLabel.includes(label)) {
        await option.click();
        return;
      }
    }
    throw Error(`Unable to check option: ${label}`);
  }

  /**
   * Gets the search input value
   */
  async getSearchValue(): Promise<string> {
    const searchInput = await this.getSearchInput();
    if (searchInput) {
      return searchInput.getValue();
    }
    return '';
  }

  /**
   * Sets the search input value
   */
  async setSearchValue(value: string): Promise<void> {
    const searchInput = await this.getSearchInput();
    await searchInput.setValue(value);
  }

  /**
   * Clears the search input
   */
  async clearSearch(): Promise<void> {
    const clearButton = await this.getClearButton();
    if (clearButton) {
      await clearButton.click();
    }
  }

  /**
   * Checks if the select is disabled
   */
  async isDisabled(): Promise<boolean> {
    const trigger = await this.getTrigger();
    return trigger.hasClass('disabled');
  }

  /**
   * Checks if the overlay is open
   */
  async isOpen(): Promise<boolean> {
    const overlay = await this.getOverlay();
    return overlay !== null;
  }

  /**
   * Gets the placeholder text
   */
  async getPlaceholder(): Promise<string> {
    const searchInput = await this.getSearchInput();
    if (searchInput) {
      return searchInput.getPlaceholder();
    }
    return '';
  }

  async clearSelection(): Promise<void> {
    const clearSelectionButton = await this.getClearSelectionButton();
    await clearSelectionButton.click();
  }

  private async getFormControlName() {
    const host = await this.host();
    const formControlName = host.getAttribute('formControlName');
    return formControlName ? formControlName : '';
  }

  /**
   * Simulates scrolling near the bottom of the options container to trigger load more
   */
  async scrollNearBottom(): Promise<void> {
    const optionsContainer = await this._documentRootLocator.locatorForOptional('.gio-select-search__options')();
    await optionsContainer.dispatchEvent('scroll');
  }
}
