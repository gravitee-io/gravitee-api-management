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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApiEndpointHarness extends ComponentHarness {
  static hostSelector = 'api-endpoint';

  private getNameInput = this.locatorFor(MatInputHarness.with({ selector: '#name' }));
  private getSaveButton = this.locatorFor(MatButtonHarness.with({ text: 'Validate my endpoints' }));
  private getPreviousButton = this.locatorFor(MatButtonHarness.with({ text: 'Previous' }));
  private getConfigurationToggle = this.locatorFor(MatSlideToggleHarness);
  private getWeightButton = this.locatorFor(MatInputHarness.with({ selector: '#weight' }));
  private getTenantsSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName=tenants]' }));
  private getConfigurationTab = this.locatorFor(MatTabHarness.with({ label: 'Configuration' }));
  private getHealthCheckTab = this.locatorFor(MatTabHarness.with({ label: 'Health-check' }));
  private getHealthCheckInheritToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName=inherit]' }));

  public async fillInputName(name: string) {
    return this.getNameInput().then(input => input.setValue(name));
  }

  public async getEndpointName() {
    return this.getNameInput().then(input => input.getValue());
  }

  public async isSaveButtonDisabled() {
    return this.getSaveButton().then(button => button.isDisabled());
  }

  public async clickSaveButton() {
    return this.getSaveButton().then(button => button.click());
  }

  public async clickPreviousButton() {
    return this.getPreviousButton().then(button => button.click());
  }

  public async isConfigurationButtonToggled() {
    return this.getConfigurationToggle().then(toggle => toggle.isChecked());
  }

  public async toggleConfigurationButton() {
    return this.getConfigurationToggle().then(toggle => toggle.toggle());
  }

  public async isHealthCheckInheritButtonToggled() {
    return this.getHealthCheckInheritToggle().then(toggle => toggle.isChecked());
  }

  public async toggleHealthCheckInheritButton() {
    return this.getHealthCheckInheritToggle().then(toggle => toggle.toggle());
  }

  public async isWeightButtonShown(): Promise<boolean> {
    return this.getWeightButton()
      .then(_ => true)
      .catch(_ => false);
  }

  public async fillWeightButton(weight: number) {
    return this.getWeightButton().then(button => button.setValue(weight.toString()));
  }

  public async clickConfigurationTab() {
    return this.getConfigurationTab().then(tab => tab.select());
  }

  public async clickHealthCheckTab() {
    return this.getHealthCheckTab().then(tab => tab.select());
  }

  public healthCheckTabIsVisible(): Promise<boolean> {
    return this.getHealthCheckTab()
      .then(_ => true)
      .catch(_ => false);
  }

  public async selectTenant(tenant: string): Promise<void> {
    return this.getTenantsSelect().then(select => select.clickOptions({ text: tenant }));
  }
}
