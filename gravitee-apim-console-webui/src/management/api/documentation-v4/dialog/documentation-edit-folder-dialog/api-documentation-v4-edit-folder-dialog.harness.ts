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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRadioButtonHarness } from '@angular/material/radio/testing';

export class ApiDocumentationV4EditFolderDialogHarness extends ComponentHarness {
  public static hostSelector = 'api-documentation-v4-edit-folder-dialog';

  private nameInputLocator = this.locatorFor(MatInputHarness.with({ placeholder: 'Name' }));
  private radioButtonsLocator = this.locatorForAll(MatRadioButtonHarness);
  private saveButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[type=submit]' }));
  private cancelButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));

  public async getNameInput() {
    return this.nameInputLocator();
  }

  public async setName(name: string) {
    return this.getNameInput().then((input) => input.setValue(name));
  }

  public async getRadioButtons() {
    return this.radioButtonsLocator();
  }

  public async selectVisibility(visibility: 'PUBLIC' | 'PRIVATE') {
    const buttons = await this.locatorForAll(
      MatRadioButtonHarness.with({
        label: new RegExp(`${visibility}.*`, 'i'),
      }),
    )();
    expect(buttons.length).toEqual(1);
    return await buttons[0].check();
  }

  public getSaveButton() {
    return this.saveButtonLocator();
  }

  public async clickOnSave() {
    return this.getSaveButton().then(async (b) => b.click());
  }
  public getCancelButton() {
    return this.cancelButtonLocator();
  }

  public async clickOnCancel() {
    return this.getCancelButton().then((b) => b.click());
  }
}
