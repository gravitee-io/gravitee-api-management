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
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApplicationSubscriptionCreationDialogHarness extends ComponentHarness {
  static hostSelector = 'application-subscription-creation-dialog';
  private getApiInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="selectedApi"]' }));
  private getApiAutoComplete = this.locatorFor(MatAutocompleteHarness);
  private getPlansRadioGroup = this.locatorFor(MatRadioGroupHarness.with({ selector: '[formControlName="selectedPlan"]' }));
  private getApiKeyModeRadioGroup = this.locatorForOptional(MatRadioGroupHarness.with({ selector: '[formControlName="apiKeyMode"]' }));
  private getCreateButton = this.locatorFor(MatButtonHarness.with({ text: 'Create' }));
  private getEntrypointSelect = this.locatorForOptional(MatSelectHarness.with({ selector: '[formControlName="selectedEntrypoint"]' }));
  private getRequestInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName="request"]' }));

  public async searchApi(apiName: string) {
    const matInputHarness = await this.getApiInput();
    await matInputHarness.setValue(apiName);
    return await matInputHarness.blur();
  }

  public async selectApi(applicationName: string) {
    return this.getApiAutoComplete().then(autocomplete => autocomplete.selectOption({ text: new RegExp(`.*${applicationName}.*`) }));
  }

  public async selectPlan(planName: string) {
    return this.getPlansRadioGroup().then(radio => radio.checkRadioButton({ label: planName }));
  }

  public async isPlanDisabled(planName: string) {
    const group = await this.getPlansRadioGroup();
    const buttons = await group.getRadioButtons();
    const options = await parallel(() => buttons.map(async option => ({ text: await option.getLabelText(), option })));
    const option = options.find(option => option.text === planName).option;
    return option.isDisabled();
  }

  public async selectApiKeyMode(mode: string) {
    return this.getApiKeyModeRadioGroup().then(radio => radio.checkRadioButton({ label: mode }));
  }

  public async createSubscription() {
    return this.getCreateButton().then(btn => btn.click());
  }

  public async isCreateSubscriptionDisabled() {
    return this.getCreateButton().then(btn => btn.isDisabled());
  }

  public async selectEntrypoint(name: string) {
    return this.getEntrypointSelect().then(select => select.clickOptions({ text: name }));
  }

  public async addRequestMessage(message: string) {
    return this.getRequestInput().then(input => input.setValue(message));
  }
}
