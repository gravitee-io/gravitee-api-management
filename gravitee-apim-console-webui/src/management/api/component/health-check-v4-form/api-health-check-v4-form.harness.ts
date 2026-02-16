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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export class ApiHealthCheckV4FormHarness extends ComponentHarness {
  static hostSelector = 'api-health-check-v4-form';

  private getEnableToggle = this.locatorForOptional(MatSlideToggleHarness.with({ selector: '[formControlName=enabled]' }));
  private getConfigurationInput = (id: string) => this.locatorForOptional(MatInputHarness.with({ selector: `[id*="${id}"]` }))();

  public async getEnableToggleValue(): Promise<boolean> {
    return this.getEnableToggle().then(toggle => toggle.isChecked());
  }

  public async toggleEnableInput(): Promise<void> {
    return this.getEnableToggle().then(toggle => toggle.toggle());
  }

  async getConfigurationInputValue(inputId: string): Promise<string> {
    return this.getConfigurationInput(inputId).then(input => input.getValue());
  }

  async setConfigurationInputValue(inputId: string, text: string): Promise<void> {
    await this.getConfigurationInput(inputId).then(input => input.setValue(text));
    await this.waitForTasksOutsideAngular();
  }

  async isConfigurationInputDisabled(inputId: string): Promise<boolean> {
    return this.getConfigurationInput(inputId).then(input => input.isDisabled());
  }
}
