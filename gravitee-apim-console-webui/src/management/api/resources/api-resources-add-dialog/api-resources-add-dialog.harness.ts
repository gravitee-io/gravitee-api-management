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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate, parallel } from '@angular/cdk/testing';
import { MatDialogSection } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';
import { MatChipListboxHarness } from '@angular/material/chips/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export interface ApiResourcesAddDialogHarnessOptions extends BaseHarnessFilters {}

export class ApiResourcesAddDialogHarness extends ComponentHarness {
  public static readonly hostSelector = `api-resources-add-dialog`;

  public static with(options: ApiResourcesAddDialogHarnessOptions): HarnessPredicate<ApiResourcesAddDialogHarness> {
    return new HarnessPredicate(ApiResourcesAddDialogHarness, options);
  }

  protected _title = this.locatorForOptional(MatDialogSection.TITLE);
  protected _content = this.locatorForOptional(MatDialogSection.CONTENT);
  protected _actions = this.locatorForOptional(MatDialogSection.ACTIONS);

  protected getRadioGroup = this.locatorFor(MatRadioGroupHarness);
  protected getFilterCategoryListbox = this.locatorForOptional(MatChipListboxHarness.with({ selector: '[formControlName="category"]' }));
  protected getFilterSearchInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="search"]' }));

  public async getTitleText(): Promise<string> {
    return (await this._title())?.text() ?? '';
  }

  public async getContentText(): Promise<string> {
    return (await this._content())?.text() ?? '';
  }

  public async getActionsText(): Promise<string> {
    return (await this._actions())?.text() ?? '';
  }

  public async close(): Promise<void> {
    const closeButton = await this.locatorFor(MatButtonHarness.with({ text: /Close/ }))();
    await closeButton.click();
  }

  public async select(resourceName: string): Promise<void> {
    // Select the radio button with the given label
    const radioGroup = await this.getRadioGroup();
    await radioGroup.checkRadioButton({ label: resourceName });

    // Confirm the selection
    const confirmButton = await this.locatorFor(MatButtonHarness.with({ text: /Select/ }))();
    await confirmButton.click();
  }

  public async getDisplayedResources(): Promise<string[]> {
    const radioGroup = await this.getRadioGroup();
    const buttons = await radioGroup.getRadioButtons();

    return parallel(() => buttons.map((button) => button.getLabelText()));
  }

  public async toggleFilterByCategory(category: string): Promise<void> {
    const filtersCategoryListbox = await this.getFilterCategoryListbox();

    if (filtersCategoryListbox) {
      const chip = await filtersCategoryListbox.getChips({
        text: category,
      });
      if (chip && chip[0]) {
        const isSelected = await chip[0].isSelected();
        isSelected ? await chip[0].deselect() : await chip[0].select();
      }
    }
  }

  public async searchFilter(search: string): Promise<void> {
    const searchInput = await this.getFilterSearchInput();
    await searchInput.setValue(search);
  }
}
