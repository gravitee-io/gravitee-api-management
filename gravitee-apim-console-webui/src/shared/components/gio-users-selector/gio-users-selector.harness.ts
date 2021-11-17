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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';

/** A set of criteria that can be used to filter a list of `GioUsersSelectorHarness` instances. */
export type GioUsersSelectorHarnessFilters = BaseHarnessFilters;

export class GioUsersSelectorHarness extends ComponentHarness {
  static hostSelector = 'gio-users-selector';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioSaveBarHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  static with(options: GioUsersSelectorHarnessFilters = {}): HarnessPredicate<GioUsersSelectorHarness> {
    return new HarnessPredicate(GioUsersSelectorHarness, options);
  }

  protected getSubmitButton = this.locatorFor(MatButtonHarness.with({ text: 'Select' }));
  protected getCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  protected getSearchAutocomplete = this.locatorFor(MatAutocompleteHarness);

  async typeSearch(searchTerm: string) {
    const searchAutocomplete = await this.getSearchAutocomplete();
    await searchAutocomplete.enterText(searchTerm);
  }

  async selectUser(displayName: string) {
    const searchAutocomplete = await this.getSearchAutocomplete();
    await searchAutocomplete.selectOption({ text: displayName });
  }

  async validate(): Promise<void> {
    const submitButton = await this.getSubmitButton();
    await submitButton.click();
  }

  async close(): Promise<void> {
    const cancelButton = await this.getCancelButton();
    await cancelButton.click();
  }
}
