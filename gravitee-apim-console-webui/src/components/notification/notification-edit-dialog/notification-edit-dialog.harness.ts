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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

export class NotificationEditDialogHarness extends ComponentHarness {
  static hostSelector = 'notification-edit-dialog';

  private getSaveBtn = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=save-button]' }));
  private getCancelBtn = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=cancel-button]' }));
  private getAllHookCheckboxes = this.locatorForAll(MatCheckboxHarness);

  public getNotifierConfigInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName=config]' }));
  public getUseSystemProxyToggle = this.locatorForOptional(MatSlideToggleHarness.with({ selector: '[formControlName=useSystemProxy]' }));

  public async getHookCheckbox(name: string) {
    return this.locatorForOptional(MatCheckboxHarness.with({ name }))();
  }
  public async getAllHooks() {
    const all = await this.getAllHookCheckboxes();
    return Promise.all(
      all.map(checkbox =>
        checkbox.getName().then(name =>
          checkbox.isChecked().then(isChecked => ({
            name,
            checked: isChecked,
          })),
        ),
      ),
    );
  }

  public async fillConfig(value: string): Promise<void> {
    const input = await this.getNotifierConfigInput();
    await input.setValue(value);
  }

  public async toggleUseSystemProxy(): Promise<void> {
    const toggle = await this.getUseSystemProxyToggle();
    await toggle.toggle();
  }

  public async save(): Promise<void> {
    const button = await this.getSaveBtn();
    await button.click();
  }

  public async cancel(): Promise<void> {
    const button = await this.getCancelBtn();
    await button.click();
  }
}
