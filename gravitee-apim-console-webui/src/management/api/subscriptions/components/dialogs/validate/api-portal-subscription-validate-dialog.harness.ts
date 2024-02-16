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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';

import { ApiKeyValidationHarness } from '../../api-key-validation/api-key-validation.harness';

export class ApiPortalSubscriptionValidateDialogHarness extends MatDialogHarness {
  static override hostSelector = 'api-portal-subscription-validate-dialog';
  protected getCustomApiKeyInput = this.locatorForOptional(ApiKeyValidationHarness);
  protected getCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  public getValidateButton = this.locatorFor(MatButtonHarness.with({ text: 'Validate' }));

  // Custom API Key
  public async isCustomApiKeyInputDisplayed() {
    const matInputHarness = await this.getCustomApiKeyInput();
    return matInputHarness !== null;
  }

  public async setCustomApiKey(customApiKey: string) {
    const matInputHarness = await this.getCustomApiKeyInput();
    return await matInputHarness.setInputValue(customApiKey);
  }

  public async getCustomApiKey() {
    const matInputHarness = await this.getCustomApiKeyInput();
    return await matInputHarness.getInputValue();
  }

  // Action buttons
  public async cancelSubscription() {
    const matButtonHarness = await this.getCancelButton();
    return await matButtonHarness.click();
  }

  public async validateSubscription() {
    const matButtonHarness = await this.getValidateButton();
    return await matButtonHarness.click();
  }
}
