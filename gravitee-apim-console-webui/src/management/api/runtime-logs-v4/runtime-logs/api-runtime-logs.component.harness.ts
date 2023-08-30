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
import { MatTabHarness } from '@angular/material/tabs/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class ApiRuntimeLogsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs';

  private getRuntimeLogsTab = this.locatorFor(MatTabHarness.with({ label: 'Runtime Logs' }));
  private getSettingsTab = this.locatorFor(MatTabHarness.with({ label: 'Settings' }));

  private getOpenLogSettingsButton = this.locatorFor(
    MatButtonHarness.with({
      text: 'Open log settings',
    }),
  );

  private getDivContainingEmptyRuntimeLogsTitle = this.locatorFor(
    DivHarness.with({ selector: '[aria-label="No runtime logs available"]' }),
  );

  public async clickRuntimeLogsTab() {
    return this.getRuntimeLogsTab().then((tab) => tab.select());
  }

  public async readRuntimeLogsTabLabel() {
    return this.getRuntimeLogsTab().then((tab) => tab.getLabel());
  }

  public async clickSettingsTab() {
    return this.getSettingsTab().then((tab) => tab.select());
  }

  public async readSettingsTabLabel() {
    return this.getSettingsTab().then((tab) => tab.getLabel());
  }

  public async getOpenLogSettingsButtonText() {
    return this.getOpenLogSettingsButton().then((button) => button.getText());
  }

  public async getEmptyRuntimeLogsTitle() {
    return this.getDivContainingEmptyRuntimeLogsTitle().then((button) => button.getText());
  }
}
