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

export class PropertiesAddDialogHarness extends ComponentHarness {
  public static hostSelector = 'properties-add-dialog';

  protected getMatInputHarness = (formControlName: string) =>
    this.locatorFor(MatInputHarness.with({ selector: `[formControlName="${formControlName}"]` }))();

  protected getMatToggleHarness = (formControlName: string) =>
    this.locatorFor(MatSlideToggleHarness.with({ selector: `[formControlName="${formControlName}"]` }))();

  async setPropertyValue({ key, value, encryptable }: { key?: string; value?: string; encryptable?: boolean }): Promise<void> {
    if (key) {
      await this.getMatInputHarness('key').then(input => input.setValue(key));
    }
    if (value) {
      await this.getMatInputHarness('value').then(input => input.setValue(value));
    }
    if (encryptable) {
      await this.getMatToggleHarness('toEncrypt').then(toggle => (encryptable ? toggle.check() : toggle.uncheck()));
    }
  }

  async add(): Promise<void> {
    await this.locatorFor(MatButtonHarness.with({ text: 'Add' }))().then(btn => btn.click());
  }
}
