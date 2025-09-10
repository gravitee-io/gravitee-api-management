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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatErrorHarness } from '@angular/material/form-field/testing';
import { GioFormSelectionInlineCardHarness, GioFormSelectionInlineHarness } from '@gravitee/ui-particles-angular';

export class CreateIntegrationHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-create-integration';

  private radioButtonsGroupLocator: AsyncFactoryFn<GioFormSelectionInlineHarness> = this.locatorFor(GioFormSelectionInlineHarness);

  private radioCards: AsyncFactoryFn<GioFormSelectionInlineCardHarness[]> = this.locatorForAll(GioFormSelectionInlineCardHarness);

  private submitStepFirstButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid=create-integration-submit-first-step]' }),
  );

  private nameInputLocator: AsyncFactoryFn<MatInputHarness> = this.locatorFor(
    MatInputHarness.with({ selector: '[data-testid=create-integration-name-input]' }),
  );
  private descriptionTextAreaLocator: AsyncFactoryFn<MatInputHarness> = this.locatorFor(
    MatInputHarness.with({ selector: '[data-testid=create-integration-description]' }),
  );
  private submitButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid=create-integration-submit-button]' }),
  );
  private addWellKnownUrl: AsyncFactoryFn<MatButtonHarness> = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid=create-integration-add-url]' }),
  );
  private wellKnownUrlInputLocator: AsyncFactoryFn<MatInputHarness> = this.locatorFor(
    MatInputHarness.with({ selector: '[data-testid=create-integration-well-known-url]' }),
  );

  public getSubmitStepFirstButton = async (): Promise<MatButtonHarness> => {
    return await this.submitStepFirstButtonLocator();
  };
  public getRadioButtonsGroup = async (): Promise<GioFormSelectionInlineHarness> => {
    return await this.radioButtonsGroupLocator();
  };

  public getRadioCards = async (): Promise<GioFormSelectionInlineCardHarness[]> => {
    return await this.radioCards();
  };

  public async setName(name: string) {
    return this.nameInputLocator().then((input: MatInputHarness) => input.setValue(name));
  }

  public async setDescription(description: string) {
    return this.descriptionTextAreaLocator().then((input: MatInputHarness) => input.setValue(description));
  }

  public async setWellKnownUrl(wellKnownUrl: string): Promise<void> {
    return this.wellKnownUrlInputLocator().then((input: MatInputHarness) => input.setValue(wellKnownUrl));
  }

  public async clickOnSubmit() {
    return this.submitButtonLocator().then(async (button: MatButtonHarness) => button.click());
  }

  public async clickOnAddNewUrl() {
    return this.addWellKnownUrl().then(async (button: MatButtonHarness) => button.click());
  }

  public async matErrorMessage(): Promise<MatErrorHarness> {
    return this.locatorFor(MatErrorHarness)();
  }
}
