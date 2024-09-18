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

export class CustomUserFieldsDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-custom-user-fields-dialog';

  private keyInputLocator = this.locatorFor(MatInputHarness.with({ selector: '[data-testid=key-input]' }));
  private labelInputLocator = this.locatorFor(MatInputHarness.with({ selector: '[data-testid=label-input]' }));
  private saveButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=dialog-save]' }));

  public async getKeyInput() {
    return this.keyInputLocator();
  }

  public async getLabelInput() {
    return this.labelInputLocator();
  }

  public async setKey(name: string) {
    return this.getKeyInput().then((input) => input.setValue(name));
  }

  public async setLabel(value: string) {
    return this.getLabelInput().then((input) => input.setValue(value));
  }

  public getSaveButton() {
    return this.saveButtonLocator();
  }

  public async clickOnSave() {
    return this.getSaveButton().then(async (b) => b.click());
  }
}
