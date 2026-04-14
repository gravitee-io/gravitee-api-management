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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

export class ClusterGeneralHarness extends ComponentHarness {
  static hostSelector = 'cluster-general';

  private getNameInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="name"]' }));
  private getDescriptionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="description"]' }));
  private getSaveBar = this.locatorFor(GioSaveBarHarness);
  private getDeleteButton = this.locatorFor(MatButtonHarness.with({ text: 'Delete' }));

  async getNameValue(): Promise<string> {
    const input = await this.getNameInput();
    return input.getValue();
  }

  async setNameValue(value: string): Promise<void> {
    const input = await this.getNameInput();
    return input.setValue(value);
  }

  async getDescriptionValue(): Promise<string> {
    const input = await this.getDescriptionInput();
    return input.getValue();
  }

  async setDescriptionValue(value: string): Promise<void> {
    const input = await this.getDescriptionInput();
    return input.setValue(value);
  }

  async isFormValid(): Promise<boolean> {
    const saveBar = await this.getSaveBar();
    return !(await saveBar.isSubmitButtonInvalid());
  }

  async submitForm(): Promise<void> {
    const saveBar = await this.getSaveBar();
    return saveBar.clickSubmit();
  }

  async clickDeleteButton(): Promise<void> {
    const deleteButton = await this.getDeleteButton();
    return deleteButton.click();
  }
}
