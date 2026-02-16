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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { ApiEndpointGroupGeneralHarness } from './general/api-endpoint-group-general.harness';

import { ApiHealthCheckV4FormHarness } from '../../component/health-check-v4-form/api-health-check-v4-form.harness';

export class ApiEndpointGroupHarness extends ComponentHarness {
  static hostSelector = 'api-endpoint-group';

  private getBackButton = this.locatorFor(MatButtonHarness.with({ selector: '[mattooltip="Go back"]' }));
  private getGeneralTab = this.locatorFor(MatTabHarness.with({ label: 'General' }));
  private getConfigurationTab = this.locatorFor(MatTabHarness.with({ label: 'Configuration' }));
  private getHealthCheckTab = this.locatorFor(MatTabHarness.with({ label: 'Health-check' }));
  private getEndpointGroupGeneralHarness = this.locatorFor(ApiEndpointGroupGeneralHarness);
  private getHealthCheckGeneralHarness = this.locatorFor(ApiHealthCheckV4FormHarness);
  private getEndpointGroupSubmissionBar = this.locatorFor(GioSaveBarHarness);

  public async clickBackButton() {
    return this.getBackButton().then(button => button.click());
  }

  public async clickGeneralTab() {
    return this.getGeneralTab().then(tab => tab.select());
  }

  public async readEndpointGroupNameInput() {
    return this.getEndpointGroupGeneralHarness().then(harness => harness.getNameValue());
  }

  public writeToEndpointGroupNameInput(inputValue) {
    return this.getEndpointGroupGeneralHarness().then(harness => harness.setNameValue(inputValue));
  }

  public async isEndpointGroupLoadBalancerSelectorShown() {
    return this.getEndpointGroupGeneralHarness()
      .then(harness => harness.getLoadBalancerValue())
      .then(_ => true)
      .catch(_ => false);
  }

  public async readEndpointGroupLoadBalancerSelector() {
    return this.getEndpointGroupGeneralHarness().then(harness => harness.getLoadBalancerValue());
  }

  public async writeToEndpointGroupLoadBalancerSelector(selectorValue) {
    return this.getEndpointGroupGeneralHarness().then(harness => harness.setLoadBalancerValue(selectorValue));
  }

  public isGeneralTabSaveButtonInvalid() {
    return this.getEndpointGroupSubmissionBar().then(gioSaveBar => gioSaveBar.isSubmitButtonInvalid());
  }

  public clickEndpointGroupSaveButton() {
    return this.getEndpointGroupSubmissionBar().then(saveBar => saveBar.clickSubmit());
  }

  public clickEndpointGroupDismissButton() {
    return this.getEndpointGroupSubmissionBar().then(saveBar => saveBar.clickReset());
  }

  public async clickHealthCheckTab() {
    return this.getHealthCheckTab().then(tab => tab.select());
  }

  public async toggleEnableHealthCheckInput() {
    return this.getHealthCheckGeneralHarness().then(harness => harness.toggleEnableInput());
  }

  public async isHealthCheckConfigurationInputDisabled(inputId: string): Promise<boolean> {
    return this.getHealthCheckGeneralHarness().then(harness => harness.isConfigurationInputDisabled(inputId));
  }

  public async readHealthCheckConfigurationValueInput(inputId: string) {
    return this.getHealthCheckGeneralHarness().then(harness => harness.getConfigurationInputValue(inputId));
  }

  public writeToHealthCheckConfigurationValueInput(inputId: string, inputValue) {
    return this.getHealthCheckGeneralHarness().then(harness => harness.setConfigurationInputValue(inputId, inputValue));
  }

  public configurationTabIsVisible(): Promise<boolean> {
    return this.getConfigurationTab()
      .then(_ => true)
      .catch(_ => false);
  }

  public healthCheckTabIsVisible(): Promise<boolean> {
    return this.getHealthCheckTab()
      .then(_ => true)
      .catch(_ => false);
  }
}
