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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ClusterConfigurationHarness extends ComponentHarness {
  static hostSelector = 'cluster-configuration';

  private getBootstrapServersInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="bootstrapServers"]' }));
  private getSecurityTypeFormField = this.locatorForOptional(MatFormFieldHarness.with({ floatingLabelText: /Security protocol/ }));
  private getSaveBar = this.locatorFor(GioSaveBarHarness);

  async getBootstrapServersValue(): Promise<string> {
    const input = await this.getBootstrapServersInput();
    return input.getValue();
  }

  async setBootstrapServersValue(value: string): Promise<void> {
    const input = await this.getBootstrapServersInput();
    return input.setValue(value);
  }

  async getSecurityTypeValue(): Promise<string> {
    const securityTypeFormField = await this.getSecurityTypeFormField();
    const securityTypeInput = (await securityTypeFormField.getControl()) as MatSelectHarness;

    if (securityTypeInput) {
      return securityTypeInput.getValueText();
    }
    return null;
  }

  async setSecurityTypeValue(value: string): Promise<void> {
    const securityTypeFormField = await this.getSecurityTypeFormField();
    const securityTypeInput = (await securityTypeFormField.getControl()) as MatSelectHarness;
    if (securityTypeInput) {
      return securityTypeInput.clickOptions({ text: value });
    }
  }

  async isFormValid(): Promise<boolean> {
    const saveBar = await this.getSaveBar();
    return !(await saveBar.isSubmitButtonInvalid());
  }

  async submitForm(): Promise<void> {
    const saveBar = await this.getSaveBar();
    return saveBar.clickSubmit();
  }
}
