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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioFormSelectionInlineHarness, GioFormTagsInputHarness } from '@gravitee/ui-particles-angular';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApplicationCreationFormHarness extends ComponentHarness {
  static readonly hostSelector = 'application-creation-form';

  private getNameInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="name"]' }));
  private getDescriptionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="description"]' }));
  private getDomainInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="domain"]' }));

  private getTypeInput = this.locatorFor(GioFormSelectionInlineHarness.with({ selector: '[formControlName="type"]' }));

  // Simple type
  private getAppTypeInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="appType"]' }));
  private getAppClientCertificateInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="appClientCertificate"]' }));
  private getAppClientIdInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="appClientId"]' }));

  // OAuth type
  private getOauthGrantTypesInput = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="oauthGrantTypes"]' }));
  public getOauthRedirectUrisInput = this.locatorForOptional(
    GioFormTagsInputHarness.with({ selector: '[formControlName="oauthRedirectUris"]' }),
  );

  public async setGeneralInformation(name: string, description: string, domain: string) {
    await this.getNameInput().then(async (input) => await input.setValue(name));
    await this.getDescriptionInput().then(async (input) => await input.setValue(description));
    await this.getDomainInput().then(async (input) => await input.setValue(domain));
  }

  public async setApplicationType(type: string) {
    await this.getTypeInput().then(async (input) => await input.select(type));
  }

  public async setSimpleApplicationType(appType: string, appClientId: string) {
    await this.getAppTypeInput().then(async (input) => await input.setValue(appType));
    await this.getAppClientIdInput().then(async (input) => await input.setValue(appClientId));
  }

  public async setOAuthApplicationType(grantTypes: string[], redirectUris?: string[]) {
    const oauthGrantTypesInput = await this.getOauthGrantTypesInput();

    await parallel(() => grantTypes.map(async (grantType) => await oauthGrantTypesInput.clickOptions({ text: grantType })));

    if (redirectUris) {
      const redirectUrisInput = await this.getOauthRedirectUrisInput();
      await parallel(() => redirectUris.map(async (redirectUri) => await redirectUrisInput.addTag(redirectUri)));
    }
  }

  public async setApplicationClientCertificate(appClientCertificate: string) {
    await this.getAppClientCertificateInput().then(async (input) => await input.setValue(appClientCertificate));
  }
}
