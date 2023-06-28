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

export class ApiEndpointHarness extends ComponentHarness {
  static hostSelector = 'api-endpoint';

  private getNameInput = this.locatorFor(MatInputHarness.with({ selector: '#name' }));
  private getSaveButton = this.locatorFor(MatButtonHarness.with({ text: 'Validate my endpoints' }));
  private getPreviousButton = this.locatorFor(MatButtonHarness.with({ text: 'Previous' }));
  private getConfigurationToggle = this.locatorFor(MatSlideToggleHarness);
  private getWeightButton = this.locatorFor(MatInputHarness.with({ selector: '#weight' }));
  private getConfigurationTab = this.locatorFor(MatTabHarness.with({ label: 'Configuration' }));

  public async fillInputName(name: string) {
    return this.getNameInput().then((input) => input.setValue(name));
  }

  public async getEndpointName() {
    return this.getNameInput().then((input) => input.getValue());
  }

  public async isSaveButtonDisabled() {
    return this.getSaveButton().then((button) => button.isDisabled());
  }

  public async clickSaveButton() {
    return this.getSaveButton().then((button) => button.click());
  }

  public async clickPreviousButton() {
    return this.getPreviousButton().then((button) => button.click());
  }

  public async isConfigurationButtonToggled() {
    return this.getConfigurationToggle().then((toggle) => toggle.isChecked());
  }

  public async toggleConfigurationButton() {
    return this.getConfigurationToggle().then((toggle) => toggle.toggle());
  }

  public async fillWeightButton(weight: number) {
    return this.getWeightButton().then((button) => button.setValue(weight.toString()));
  }

  public async clickConfigurationTab() {
    return this.getConfigurationTab().then((tab) => tab.select());
  }
}
