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
import { MatSelectHarness } from '@angular/material/select/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

export class ClusterConfigurationHarness extends ComponentHarness {
  static hostSelector = 'cluster-configuration';

  private getJsonSchemaForm = this.locatorForOptional('gio-form-json-schema');
  private getSaveBar = this.locatorFor(GioSaveBarHarness);

  async isJsonSchemaFormPresent(): Promise<boolean> {
    return (await this.getJsonSchemaForm()) !== null;
  }

  async getBootstrapServersInput(): Promise<MatInputHarness | null> {
    return this.locatorForOptional(MatInputHarness.with({ selector: '[id*="bootstrapServers"]' }))();
  }

  async getBootstrapServersValue(): Promise<string> {
    const input = await this.getBootstrapServersInput();
    return input ? input.getValue() : null;
  }

  async setBootstrapServersValue(value: string): Promise<void> {
    const input = await this.getBootstrapServersInput();
    return input.setValue(value);
  }

  async getSecurityTypeValue(): Promise<string> {
    const select = await this.locatorForOptional(MatSelectHarness.with({ selector: '[id*="protocol"]' }))();
    return select ? select.getValueText() : null;
  }

  async setSecurityTypeValue(value: string): Promise<void> {
    const select = await this.locatorForOptional(MatSelectHarness.with({ selector: '[id*="protocol"]' }))();
    if (select) {
      return select.clickOptions({ text: value });
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
