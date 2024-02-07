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
import { MatSelectHarness } from '@angular/material/select/testing';

export class NotificationAddDialogHarness extends ComponentHarness {
  static hostSelector = 'notification-add-dialog';

  private getConfirmBtn = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=create-button]' }));
  private getCancelBtn = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=cancel-button]' }));

  private getNameInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName=name]' }));
  private getNotifierSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName=notifierId]' }));

  public async fillForm(name: string, notifierName: string) {
    const nameInput = await this.getNameInput();
    await nameInput.setValue(name);

    const notifierSelect = await this.getNotifierSelect();
    await notifierSelect.clickOptions({ text: notifierName });
  }

  public async confirm(): Promise<void> {
    const button = await this.getConfirmBtn();
    await button.click();
  }

  public async cancel(): Promise<void> {
    const button = await this.getCancelBtn();
    await button.click();
  }
}
