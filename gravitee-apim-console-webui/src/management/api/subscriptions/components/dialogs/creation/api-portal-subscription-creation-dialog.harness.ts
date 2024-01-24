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
import { MatRadioGroupHarness, RadioButtonHarnessFilters } from '@angular/material/radio/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';

import { ApiKeyValidationHarness } from '../../api-key-validation/api-key-validation.harness';

export class ApiPortalSubscriptionCreationDialogHarness extends MatDialogHarness {
  static hostSelector = 'api-portal-subscription-creation-dialog';

  protected getApplicationAutocomplete = this.locatorFor(MatAutocompleteHarness);
  protected getInputApplicationSearch = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="selectedApplication"]' }));
  public getSelectedApplicationFormField = this.locatorFor(
    MatFormFieldHarness.with({ selector: '.subscription-creation__content__applications' }),
  );
  public getPlansRadioGroup = this.locatorFor(MatRadioGroupHarness.with({ selector: '[formControlName="selectedPlan"]' }));
  protected getApiKeyModeRadioGroup = this.locatorForOptional(MatRadioGroupHarness.with({ selector: '[formControlName="apiKeyMode"]' }));
  protected getCustomApiKeyInput = this.locatorForOptional(ApiKeyValidationHarness);
  protected getSelectEntrypointSelect = this.locatorForOptional(
    MatSelectHarness.with({ selector: '[formControlName="selectedEntrypoint"]' }),
  );
  protected getChannelInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName="channel"]' }));
  protected entrypointConfigurationForm = this.locatorForOptional('gio-form-json-schema');

  protected getCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '.actions__cancelBtn' }));
  public getCreateButton = this.locatorFor(MatButtonHarness.with({ selector: '.actions__createBtn' }));

  // Applications
  public async searchApplication(applicationNameToSearch: string) {
    const matInputHarness = await this.getInputApplicationSearch();
    return await matInputHarness.setValue(applicationNameToSearch);
  }

  public async selectApplication(applicationName: string) {
    const matAutocompleteHarness = await this.getApplicationAutocomplete();
    const regex = new RegExp(`.*${applicationName}.*`);
    await matAutocompleteHarness.selectOption({ text: regex });
  }

  public async getApplicationErrors() {
    const selectedApplicationFormField = await this.getSelectedApplicationFormField();
    if (await selectedApplicationFormField.hasErrors()) {
      return await selectedApplicationFormField.getTextErrors();
    }
    return [];
  }

  // Plans
  public async getRadioButtons(filter?: RadioButtonHarnessFilters) {
    return (await this.getPlansRadioGroup()).getRadioButtons(filter);
  }

  public async choosePlan(planName: string) {
    const matRadioGroupHarness = await this.getPlansRadioGroup();
    return await matRadioGroupHarness.checkRadioButton({ label: planName });
  }

  public async isPlanRadioGroupEnabled() {
    const matRadioGroupHarness = await this.getPlansRadioGroup();
    const group = await matRadioGroupHarness.host();
    return (await group.getAttribute('ng-reflect-disabled')) !== 'true';
  }

  // Custom API Key
  public async isCustomApiKeyInputDisplayed() {
    const matInputHarness = await this.getCustomApiKeyInput();
    return matInputHarness !== null;
  }

  public async isApiKeyModeRadioGroupDisplayed() {
    const matRadioGroupHarness = await this.getApiKeyModeRadioGroup();
    return matRadioGroupHarness !== null;
  }

  public async chooseApiKeyMode(label: string) {
    const matRadioGroupHarness = await this.getApiKeyModeRadioGroup();
    return await matRadioGroupHarness.checkRadioButton({ label });
  }

  public async addCustomKey(customApikey: string) {
    const matInputHarness = await this.getCustomApiKeyInput();
    return await matInputHarness.setInputValue(customApikey);
  }

  // PUSH Plan
  public async isEntrypointSelectDisplayed() {
    const matSelectHarness = await this.getSelectEntrypointSelect();
    return matSelectHarness !== null;
  }

  public async selectEntrypoint(entrypointId: string) {
    const matSelectHarness = await this.getSelectEntrypointSelect();
    return await matSelectHarness.clickOptions({ text: entrypointId });
  }

  public async isChannelInputDisplayed() {
    const matInputHarness = await this.getChannelInput();
    return matInputHarness !== null;
  }

  public async addChannel(channel: string) {
    const matInputHarness = await this.getChannelInput();
    return await matInputHarness.setValue(channel);
  }

  public async isEntrypointConfigurationFormDisplayed() {
    return (await this.entrypointConfigurationForm()) !== null;
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
