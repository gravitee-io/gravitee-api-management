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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatCardHarness } from '@angular/material/card/testing';

export class IntegrationGeneralConfigurationHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-integration-general-configuration';

  private nameInputLocator: AsyncFactoryFn<MatInputHarness> = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid=update-integration-name-input]' }),
  );
  private descriptionTextAreaLocator: AsyncFactoryFn<MatInputHarness> = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid=update-integration-description]' }),
  );
  private submitButtonLocator: AsyncFactoryFn<GioSaveBarHarness> = this.locatorForOptional(GioSaveBarHarness);
  private deleteIntegrationButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid=delete-integration-button]' }),
  );
  private deleteFederatedApisButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid="delete-federated-apis-button"]' }),
  );

  public updateSectionLocator: AsyncFactoryFn<MatCardHarness> = this.locatorForOptional(
    MatCardHarness.with({ selector: '[data-testid=update-card]' }),
  );

  public getUpdateSection = async (): Promise<MatCardHarness> => {
    return this.updateSectionLocator();
  };

  public getSubmitButton = async (): Promise<GioSaveBarHarness> => {
    return this.submitButtonLocator();
  };

  public async setName(name: string) {
    return this.nameInputLocator().then((input: MatInputHarness) => input.setValue(name));
  }

  public async setDescription(description: string) {
    return this.descriptionTextAreaLocator().then((input: MatInputHarness) => input.setValue(description));
  }

  public async clickOnSubmit(): Promise<void> {
    return this.submitButtonLocator().then(async (button: GioSaveBarHarness) => button.clickSubmit());
  }

  public async matErrorMessage(): Promise<MatErrorHarness> {
    return this.locatorFor(MatErrorHarness)();
  }

  public getDeleteIntegrationButton = async (): Promise<MatButtonHarness> => {
    return this.deleteIntegrationButtonLocator();
  };

  public getDeleteFederatedApisButton = async (): Promise<MatButtonHarness> => {
    return this.deleteFederatedApisButtonLocator();
  };
}
