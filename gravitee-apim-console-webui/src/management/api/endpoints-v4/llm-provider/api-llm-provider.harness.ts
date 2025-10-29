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
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
// JSON schema harness not used; interact via component in tests when needed

export class ApiLlmProviderHarness extends ComponentHarness {
  static hostSelector = 'api-provider';

  private getTitleElement = this.locatorFor('.mat-h3');
  private getForm = this.locatorFor('form');
  private getProviderNameInput = this.locatorFor(MatInputHarness.with({ selector: '#name' }));
  private getProviderNameFormField = this.locatorFor(MatFormFieldHarness.with({ selector: '#name' }));
  private getSaveButton = this.locatorFor(MatButtonHarness.with({ text: 'Validate my endpoints' }));
  private getBackButton = this.locatorFor(MatButtonHarness.with({ text: 'Go back to your endpoints' }));

  public async getTitle(): Promise<string> {
    const titleElement = await this.getTitleElement();
    return await titleElement.text();
  }

  public async isFormVisible(): Promise<boolean> {
    try {
      await this.getForm();
      return true;
    } catch {
      return false;
    }
  }

  public async isFormDisabled(): Promise<boolean> {
    const input = await this.getProviderNameInput();
    return await input.isDisabled();
  }

  public async getProviderName(): Promise<string> {
    const input = await this.getProviderNameInput();
    return await input.getValue();
  }

  public async setProviderName(value: string): Promise<void> {
    const input = await this.getProviderNameInput();
    return await input.setValue(value);
  }

  public async blurProviderName(): Promise<void> {
    const input = await this.getProviderNameInput();
    return await input.blur();
  }

  public async getProviderNameError(): Promise<string> {
    const errorElement = await this.locatorForOptional('mat-form-field .mat-error')();
    if (!errorElement) return '';
    return await errorElement.text();
  }

  public async isSaveButtonDisabled(): Promise<boolean> {
    const button = await this.getSaveButton();
    return await button.isDisabled();
  }

  public async getSaveButtonText(): Promise<string> {
    const button = await this.getSaveButton();
    return await button.getText();
  }

  public async clickSaveButton(): Promise<void> {
    const button = await this.getSaveButton();
    return await button.click();
  }

  public async clickBackButton(): Promise<void> {
    const button = await this.getBackButton();
    return await button.click();
  }
}
