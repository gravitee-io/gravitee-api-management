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

export class PropertiesImportDialogHarness extends ComponentHarness {
  public static hostSelector = 'properties-import-dialog';

  protected getMatInputHarness = (formControlName: string) =>
    this.locatorFor(MatInputHarness.with({ selector: `[formControlName="${formControlName}"]` }))();

  public setProperties(properties: string): Promise<void> {
    return this.getMatInputHarness('properties').then(input => input.setValue(properties));
  }

  async getErrorMessage(): Promise<string | null> {
    const errorBanner = await this.locatorForOptional('gio-banner-error')();

    if (errorBanner) {
      return errorBanner.text();
    }

    return null;
  }

  async getWarningMessage(): Promise<string | null> {
    const warningBanner = await this.locatorForOptional('gio-banner-warning')();

    if (warningBanner) {
      return warningBanner.text();
    }

    return null;
  }

  async import(): Promise<void> {
    await this.locatorFor(MatButtonHarness.with({ text: 'Import properties' }))().then(btn => btn.click());
  }
}
