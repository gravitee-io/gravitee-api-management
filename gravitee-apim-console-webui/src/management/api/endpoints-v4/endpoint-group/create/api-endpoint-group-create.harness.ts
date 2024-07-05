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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatStepHarness } from '@angular/material/stepper/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { ApiEndpointGroupGeneralHarness } from '../general/api-endpoint-group-general.harness';
import { GioSelectionRadioListHarness } from '../../../../../shared/components/gio-selection-list-option/gio-selection-radio-list.harness';

export class ApiEndpointGroupCreateHarness extends ComponentHarness {
  static hostSelector = 'api-endpoint-group-create';

  protected getEndpointGroupRadio = this.locatorFor(GioSelectionRadioListHarness);
  protected getButtonByText = (text: string) => this.locatorFor(MatButtonHarness.with({ text }))();
  protected getEndpointGroupGeneralHarness = this.locatorFor(ApiEndpointGroupGeneralHarness);
  protected getBannerBody = this.locatorFor(DivHarness.with({ selector: '.banner__wrapper__body' }));
  protected getConfigurationInput = (id: string) => this.locatorFor(MatInputHarness.with({ selector: `[id*="${id}"]` }))();
  protected getStepByLabel = (label: string) => this.locatorFor(MatStepHarness.with({ label }))();

  // Step 1: Endpoint Group Type
  async getEndpointGroupTypeStep(): Promise<MatStepHarness> {
    return this.getStepByLabel('Endpoint Group Type');
  }

  async isEndpointGroupTypeStepSelected(): Promise<boolean> {
    return await this.getEndpointGroupTypeStep().then((step) => step.isSelected());
  }

  async selectEndpointGroup(type: string): Promise<void> {
    return this.getEndpointGroupRadio().then((radioGroup) => radioGroup.selectOptionById(type));
  }

  async getEndpointGroupTypes(): Promise<string[]> {
    return this.getEndpointGroupRadio().then((radioGroup) => radioGroup.getValues());
  }

  async getSelectedEndpointGroupId(): Promise<string> {
    return this.getEndpointGroupRadio().then((radioGroup) => radioGroup.getValue());
  }

  async getValidateEndpointGroupSelectionButton(): Promise<MatButtonHarness> {
    return this.getButtonByText('Select endpoint group type');
  }

  async canGoToGeneralStep(): Promise<boolean> {
    return await this.isButtonClickable(this.getValidateEndpointGroupSelectionButton());
  }

  async validateEndpointGroupSelection(): Promise<void> {
    const button = await this.getValidateEndpointGroupSelectionButton();
    return await button.click();
  }

  async goBackToEndpointGroups(): Promise<void> {
    return this.getButtonByText('Exit').then((btn) => btn.click());
  }

  // Step 2: General
  async getGeneralStep(): Promise<MatStepHarness> {
    return this.getStepByLabel('General');
  }

  async isGeneralStepSelected(): Promise<boolean> {
    return await this.getGeneralStep().then((step) => step.isSelected());
  }

  async getNameValue(): Promise<string> {
    return this.getEndpointGroupGeneralHarness().then((harness) => harness.getNameValue());
  }

  async setNameValue(text: string): Promise<void> {
    return this.getEndpointGroupGeneralHarness().then((harness) => harness.setNameValue(text));
  }

  public async getLoadBalancerValue() {
    return this.getEndpointGroupGeneralHarness().then((harness) => harness.getLoadBalancerValue());
  }

  public async setLoadBalancerValue(value: string) {
    return this.getEndpointGroupGeneralHarness().then((harness) => harness.setLoadBalancerValue(value));
  }

  async getValidateGeneralInformationButton(): Promise<MatButtonHarness> {
    return this.getButtonByText('Validate general information');
  }

  async canGoToConfigurationStep(): Promise<boolean> {
    return await this.isButtonClickable(this.getValidateGeneralInformationButton());
  }
  async validateGeneralInformation(): Promise<void> {
    return this.getValidateGeneralInformationButton().then((btn) => btn.click());
  }

  // Step 3: Configuration
  async getConfigurationStep(): Promise<MatStepHarness> {
    return this.getStepByLabel('Configuration');
  }

  async isConfigurationStepValid(): Promise<boolean> {
    const step = await this.getConfigurationStep();
    return !(await step.hasErrors());
  }

  async isEndpointGroupMockBannerShown(): Promise<boolean> {
    return this.bannerBodyContainsText('Mock');
  }

  async isInheritedConfigurationBannerShown(): Promise<boolean> {
    return this.bannerBodyContainsText('inherit');
  }

  async isConfigurationFormShown(): Promise<boolean> {
    return this.locatorFor('gio-form-json-schema')()
      .then((_) => true)
      .catch((_) => false);
  }

  async getConfigurationInputValue(inputId: string): Promise<string> {
    return this.getConfigurationInput(inputId).then((input) => input.getValue());
  }

  async setConfigurationInputValue(inputId: string, text: string): Promise<void> {
    await this.getConfigurationInput(inputId).then((input) => input.setValue(text));
    await this.waitForTasksOutsideAngular();
  }

  async getCreateEndpointGroupButton(): Promise<MatButtonHarness> {
    return this.getButtonByText('Create endpoint group');
  }

  async getCreateDlqEndpointGroupButton(): Promise<MatButtonHarness> {
    return this.getButtonByText('Create DLQ endpoint group');
  }

  async canCreateEndpointGroup(): Promise<boolean> {
    return await this.isButtonClickable(this.getCreateEndpointGroupButton());
  }

  async createEndpointGroup(): Promise<void> {
    return this.getCreateEndpointGroupButton().then((btn) => btn.click());
  }

  async createDlqEndpointGroup(): Promise<void> {
    return this.getCreateDlqEndpointGroupButton().then((btn) => btn.click());
  }

  private async bannerBodyContainsText(text: string): Promise<boolean> {
    return this.getBannerBody()
      .then((div) => div.getText())
      .then((txt) => txt.includes(text));
  }

  private async isButtonClickable(button: Promise<MatButtonHarness>): Promise<boolean> {
    const btn = await button;
    return !(await btn.isDisabled());
  }
}
