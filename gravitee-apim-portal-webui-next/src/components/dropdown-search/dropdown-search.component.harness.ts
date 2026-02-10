/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness, ContentContainerComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export class DropdownSearchOptionHarness extends ContentContainerComponentHarness {
  public static hostSelector = '.portal-dropdown-search-overlay__option';

  private getLabel = this.locatorFor('.portal-dropdown-search-overlay__option-label');

  public async getLabelText(): Promise<string> {
    const label = await this.getLabel();
    return label.text();
  }
}

/** Harness for the dropdown overlay (options panel). Use via DropdownSearchComponentHarness.getOverlayHarness()*/
export class DropdownSearchOverlayHarness extends ComponentHarness {
  public static hostSelector = 'app-dropdown-search-overlay';

  private getOptionElements = this.locatorForAll(
    '.portal-dropdown-search-overlay__option:not(.portal-dropdown-search-overlay__option--disabled)',
  );

  /** Returns the label text of each selectable option. */
  public async getOptionLabels(): Promise<string[]> {
    const options = await this.getOptionElements();
    return Promise.all(options.map(opt => opt.text()));
  }

  /** Clicks the option at the given index (0-based). */
  public async selectOptionByIndex(index: number): Promise<void> {
    const options = await this.getOptionElements();
    if (options[index]) {
      await options[index].click();
    }
  }

  /** Clicks the option whose label contains the given text. */
  public async selectOptionByLabel(label: string): Promise<void> {
    const options = await this.getOptionElements();
    for (const opt of options) {
      const text = await opt.text();
      if (text.includes(label)) {
        await opt.click();
        return;
      }
    }
  }

  /** Clicks options for each label in the list (for multi-select). */
  public async selectOptionsByLabels(labels: string[]): Promise<void> {
    for (const label of labels) {
      await this.selectOptionByLabel(label);
    }
  }
}

export class DropdownSearchComponentHarness extends ComponentHarness {
  public static hostSelector = 'app-dropdown-search';

  private getTrigger = this.locatorFor('.portal-dropdown-search__trigger');
  private getOverlay = this.documentRootLocatorFactory().locatorForOptional('.portal-dropdown-search-overlay');

  /** Opens the dropdown by clicking the trigger. */
  public async open(): Promise<void> {
    const isOpen = await this.isOpen();
    if (!isOpen) {
      const trigger = await this.getTrigger();
      await trigger.click();
    }
  }

  /** Closes the dropdown by clicking the trigger again. */
  public async close(): Promise<void> {
    const isOpen = await this.isOpen();
    if (isOpen) {
      const trigger = await this.getTrigger();
      await trigger.click();
    }
  }

  /** Returns whether the dropdown overlay is open. */
  public async isOpen(): Promise<boolean> {
    const overlay = await this.getOverlay();
    return overlay !== null;
  }

  /** Opens the dropdown (if needed) and returns the overlay harness. Use this instead of passing a root loader. */
  public async getOverlayHarness(): Promise<DropdownSearchOverlayHarness> {
    await this.open();
    return this.documentRootLocatorFactory().locatorFor(DropdownSearchOverlayHarness)();
  }

  /** Returns the trigger button text (label + selected count). */
  public async getTriggerText(): Promise<string> {
    const trigger = await this.getTrigger();
    return trigger.text();
  }

  /** Sets the search term in the overlay's search input. */
  public async setSearchTerm(term: string): Promise<void> {
    await this.open();
    const input = await this.documentRootLocatorFactory().locatorFor(MatInputHarness.with({ selector: 'input[matInput]' }))();
    await input.setValue(term);
  }

  /** Returns the list of option labels currently visible in the overlay. */
  public async getOptionsLabels(): Promise<string[]> {
    await this.open();
    const options = await this.documentRootLocatorFactory().locatorForAll('.portal-dropdown-search-overlay__option-label')();
    return Promise.all(options.map(opt => opt.text()));
  }

  /** Toggles an option by its label. */
  public async toggleOption(label: string): Promise<void> {
    await this.open();
    const options = await this.documentRootLocatorFactory().locatorForAll(DropdownSearchOptionHarness)();
    for (const option of options) {
      const text = await option.getLabelText();
      if (text.includes(label)) {
        await (await option.host()).click();
        return;
      }
    }
    throw new Error(`Option with label "${label}" not found`);
  }

  /** Clears the selection via the "Clear Selection" button. */
  public async clearSelection(): Promise<void> {
    await this.open();
    const clearButton = await this.documentRootLocatorFactory().locatorFor(
      MatButtonHarness.with({ selector: '.portal-dropdown-search-overlay__clear button' }),
    )();
    await clearButton.click();
  }

  /** Returns whether the dropdown is disabled. */
  public async isDisabled(): Promise<boolean> {
    const trigger = await this.getTrigger();
    return trigger.hasClass('portal-dropdown-search__trigger--disabled');
  }

  /** Returns the selected options checkboxes. */
  public async getCheckedOptions(): Promise<MatCheckboxHarness[]> {
    await this.open();
    return this.documentRootLocatorFactory().locatorForAll(MatCheckboxHarness.with({ checked: true }))();
  }
}
