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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';

export class ApiPortalSubscriptionCreationDialogHarness extends MatDialogHarness {
  static hostSelector = 'api-portal-subscription-creation-dialog';

  protected getApplicationAutocomplete = this.locatorFor(MatAutocompleteHarness);
  protected getInputApplicationSearch = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="selectedApplication"]' }));
  protected getPlansRadioGroup = this.locatorFor(MatRadioGroupHarness.with({ selector: '[formControlName="selectedPlan"]' }));
  protected getCustomApikeyInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName="customApiKey"]' }));

  protected getCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '.actions__cancelBtn' }));
  protected getCreateButton = this.locatorFor(MatButtonHarness.with({ selector: '.actions__createBtn' }));

  public async searchApplication(applicationNameToSearch: string) {
    const matAutocompleteHarness = await this.getInputApplicationSearch();
    return await matAutocompleteHarness.setValue(applicationNameToSearch);
  }

  public async selectApplication(applicationName: string) {
    const matAutocompleteHarness = await this.getApplicationAutocomplete();
    return await matAutocompleteHarness.selectOption({ text: applicationName });
  }

  public async getRadioButtons() {
    return (await this.getPlansRadioGroup()).getRadioButtons();
  }

  public async choosePlan(planName: string) {
    const matRadioGroupHarness = await this.getPlansRadioGroup();
    return await matRadioGroupHarness.checkRadioButton({ label: planName });
  }

  public async isCustomApiKeyInputDisplayed() {
    const matInputHarness = await this.getCustomApikeyInput();
    return matInputHarness !== null;
  }

  public async addCustomKey(customApikey: string) {
    const matInputHarness = await this.getCustomApikeyInput();
    return await matInputHarness.setValue(customApikey);
  }

  public async cancelSubscription() {
    const matButtonHarness = await this.getCancelButton();
    return await matButtonHarness.click();
  }

  public async createSubscription() {
    const matButtonHarness = await this.getCreateButton();
    return await matButtonHarness.click();
  }
}
