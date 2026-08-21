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
import { AsyncFactoryFn, ComponentHarness } from '@angular/cdk/testing';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class AddApiProductToCategoryDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'add-api-product-to-category-dialog';

  private readonly inputLocator: AsyncFactoryFn<MatAutocompleteHarness> = this.locatorFor(
    MatAutocompleteHarness.with({ selector: '[data-testid=api-product-select-input]' }),
  );
  private readonly addButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid=submit-button]' }),
  );
  private readonly cancelButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid=cancel-button]' }),
  );

  public async getOptionLabels(filter?: string): Promise<string[]> {
    const inputAutocomplete = await this.inputLocator();
    await inputAutocomplete.focus();
    if (filter) {
      await inputAutocomplete.enterText(filter);
    }
    const options = await inputAutocomplete.getOptions();
    return Promise.all(options.map(option => option.getText()));
  }

  public async fillFormAndSubmit(value: string): Promise<void> {
    const inputAutocomplete = await this.inputLocator();
    await inputAutocomplete.enterText(value);
    await inputAutocomplete.selectOption({ text: new RegExp(value) });
    await this.addButtonLocator().then(button => button.click());
  }

  public async cancel(): Promise<void> {
    await this.cancelButtonLocator().then(button => button.click());
  }
}
