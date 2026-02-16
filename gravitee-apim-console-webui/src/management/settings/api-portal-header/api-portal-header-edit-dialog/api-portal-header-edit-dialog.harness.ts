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

export class ApiPortalHeaderEditDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'api-portal-header-edit-dialog';

  private nameInputLocator = this.locatorFor(MatInputHarness.with({ selector: '[data-testid=name-input]' }));
  private valueInputLocator = this.locatorFor(MatInputHarness.with({ selector: '[data-testid=value-input]' }));
  private cancelButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=cancel-button]' }));
  private saveButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=submit-button]' }));

  public async getNameInput() {
    return this.nameInputLocator();
  }

  public async getValueInput() {
    return this.valueInputLocator();
  }

  public async setName(name: string) {
    return this.getNameInput().then(input => input.setValue(name));
  }

  public async setValue(value: string) {
    return this.getValueInput().then(input => input.setValue(value));
  }

  public getSaveButton() {
    return this.saveButtonLocator();
  }

  public async clickOnSave() {
    return this.getSaveButton().then(async b => b.click());
  }
}
