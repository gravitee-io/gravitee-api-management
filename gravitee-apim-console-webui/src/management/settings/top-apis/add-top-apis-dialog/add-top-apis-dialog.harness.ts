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
import { MatOptionHarness } from '@angular/material/core/testing';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class AddTopApisDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'add-top-apis-dialog';
  private inputLocator: AsyncFactoryFn<MatAutocompleteHarness> = this.locatorFor(
    MatAutocompleteHarness.with({ selector: '[data-testid=new-top-api-input]' }),
  );
  private optionLocator: AsyncFactoryFn<MatOptionHarness> = this.locatorFor(
    MatOptionHarness.with({ selector: '[data-testid=new-top-api-option' }),
  );
  private addButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid=submit-button' }),
  );

  public async fillFormAndSubmit(value: string, onAutocompleteIsSetCb: () => void): Promise<void> {
    const inputAutocomplete = await this.inputLocator();

    await inputAutocomplete.enterText(value);
    onAutocompleteIsSetCb();

    await inputAutocomplete.selectOption({});

    await this.addButtonLocator().then((button: MatButtonHarness) => button.click());
  }

  public async selectOption(): Promise<MatOptionHarness> {
    return this.optionLocator();
  }
}
