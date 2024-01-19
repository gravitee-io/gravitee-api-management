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
import { MatLegacyButtonHarness as MatButtonHarness } from '@angular/material/legacy-button/testing';
import { MatLegacyAutocompleteHarness as MatAutocompleteHarness } from '@angular/material/legacy-autocomplete/testing';
import { MatLegacyOptionHarness as MatOptionHarness } from '@angular/material/legacy-core/testing';
import { MatLegacyInputHarness as MatInputHarness } from '@angular/material/legacy-input/testing';

/** A set of criteria that can be used to filter a list of `GioFormUserAutocompleteHarnessHarness` instances. */
export type GioFormUserAutocompleteHarnessFilters = BaseHarnessFilters;

export class GioFormUserAutocompleteHarness extends ComponentHarness {
  static hostSelector = 'gio-form-user-autocomplete';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioFormUserAutocompleteHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  static with(options: GioFormUserAutocompleteHarnessFilters = {}): HarnessPredicate<GioFormUserAutocompleteHarness> {
    return new HarnessPredicate(GioFormUserAutocompleteHarness, options);
  }

  protected getSearchAutocomplete = this.locatorFor(MatAutocompleteHarness);
  protected getInputSearch = this.locatorFor(MatInputHarness);

  protected getClearButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[aria-label="Clear"]' }));

  async getOptions(filter: { text: string | RegExp }): Promise<MatOptionHarness[]> {
    const searchAutocomplete = await this.getSearchAutocomplete();
    return await searchAutocomplete.getOptions(filter);
  }

  async selectOption(filter: { text: string | RegExp }) {
    const searchAutocomplete = await this.getSearchAutocomplete();
    await searchAutocomplete.selectOption(filter);
  }

  async getSearchValue(): Promise<string> {
    const searchInput = await this.getInputSearch();
    return await searchInput.getValue();
  }

  async setSearchText(text: string): Promise<void> {
    const searchInput = await this.getInputSearch();
    await searchInput.setValue(text);
  }

  async clear(): Promise<void> {
    const btn = await this.getClearButton();
    await btn.click();
  }
}
